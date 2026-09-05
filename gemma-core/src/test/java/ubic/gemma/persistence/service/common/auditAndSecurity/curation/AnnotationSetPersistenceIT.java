/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Integration test for the V20/V21 {@code ANNOTATION_SET} schema migration and
 * the unified {@link AnnotationSet} entity + service. Runs against the real
 * MySQL {@code gemdtest} via {@link BaseIntegrationTest5} — proves that:
 *
 * <ol>
 *   <li>Flyway migration V20/V21 applies cleanly on a fresh schema.</li>
 *   <li>The three roles (PROPOSAL/DRAFT/SNAPSHOT) and four sources persist
 *       via {@code @Enumerated(EnumType.STRING)} against the
 *       {@code VARCHAR(32)} columns.</li>
 *   <li>Idempotency on {@code (investigation, role, runId)} works:
 *       a repeat {@code attach} returns the existing row.</li>
 *   <li>{@link AnnotationSetService#upsertDraft} updates an existing
 *       {@code DRAFT} for the same {@code (investigation, curator)}
 *       rather than creating a duplicate.</li>
 *   <li>Lineage (self-FK on {@code PARENT_FK}) survives a round-trip.</li>
 *   <li>Finalize / reopen flips {@code finalizedAt} idempotently.</li>
 *   <li>The summary projection populates every field from the HQL
 *       {@code SELECT NEW ...} constructor.</li>
 * </ol>
 *
 * <p>Class-level {@link Transactional} opens a per-test transaction that
 * Spring rolls back at end-of-test, so no persistent cleanup is needed.</p>
 */
@Transactional
public class AnnotationSetPersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private AnnotationSetService annotationSetService;

    @Autowired
    private AnnotationSetDao annotationSetDao;

    @Autowired
    private SessionFactory sessionFactory;

    private PreboardedExperiment preboarded;

    @BeforeEach
    public void seedPreboarded() {
        preboarded = new PreboardedExperiment();
        preboarded.setAccession( "GSE-as-it-" + UUID.randomUUID() );
        preboarded.setSource( "GEO" );
        preboarded.setName( "AnnotationSetIT preboarded" );
        preboarded.setWorkflowState( WorkflowState.Preboarded );
        sessionFactory.getCurrentSession().persist( preboarded );
        sessionFactory.getCurrentSession().flush();
    }

    private void flushAndClear() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    @Test
    @DisplayName("PROPOSAL attach + idempotent retry on same runId")
    public void proposal_attachAndIdempotency() {
        String runId = "run-" + UUID.randomUUID();
        AnnotationSetService.AttachedAnnotationSet first = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, runId, "agent-1",
                "0.8.0", "claude-opus-4-7", null,
                "{\"factors\":[]}", null );
        assertTrue( first.isCreated(), "first attach should report created=true" );
        Long firstId = first.getAnnotationSet().getId();
        assertNotNull( firstId );
        flushAndClear();

        AnnotationSetService.AttachedAnnotationSet repeat = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, runId, "agent-1",
                "0.8.0", "claude-opus-4-7", null,
                "{\"factors\":[{\"name\":\"x\"}]}", null );
        assertEquals( false, repeat.isCreated(),
                "retry with same (role, runId) should report created=false" );
        assertEquals( firstId, repeat.getAnnotationSet().getId(),
                "retry should return the same row" );
    }

    @Test
    @DisplayName("DRAFT upsert: second call updates the same row in place")
    public void draft_upsertReplacesInPlace() {
        AnnotationSet first = annotationSetService.upsertDraft(
                preboarded, "alice", "{\"v\":1}", null, null );
        Long firstId = first.getId();
        assertNotNull( firstId );
        assertEquals( "draft-alice", first.getRunId() );
        flushAndClear();

        AnnotationSet second = annotationSetService.upsertDraft(
                preboarded, "alice", "{\"v\":2}", "[\"factor:1:0\"]", null );
        assertEquals( firstId, second.getId(),
                "second upsert should reuse the same row id (one DRAFT per curator)" );
        flushAndClear();

        AnnotationSet reloaded = annotationSetDao.load( firstId );
        assertNotNull( reloaded );
        assertEquals( "{\"v\":2}", reloaded.getPayloadJson() );
        assertEquals( "[\"factor:1:0\"]", reloaded.getParkedElements() );
    }

    @Test
    @DisplayName("DRAFT parent lineage round-trips through the self-FK")
    public void draft_parentLineageRoundTrip() {
        AnnotationSetService.AttachedAnnotationSet parent = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, "run-parent", "agent-1",
                null, null, null, "{\"src\":\"parent\"}", null );
        Long parentId = parent.getAnnotationSet().getId();

        AnnotationSet draft = annotationSetService.upsertDraft(
                preboarded, "bob", "{\"src\":\"draft\"}", null,
                parent.getAnnotationSet() );
        Long draftId = draft.getId();
        flushAndClear();

        AnnotationSet reloaded = annotationSetDao.load( draftId );
        assertNotNull( reloaded );
        assertNotNull( reloaded.getParent(), "draft parent should round-trip" );
        assertEquals( parentId, reloaded.getParent().getId() );
    }

    @Test
    @DisplayName("SNAPSHOT with generated UUID runId is append-only across calls")
    public void snapshot_appendOnly() {
        AnnotationSetService.AttachedAnnotationSet a = annotationSetService.attach(
                preboarded, AnnotationSetRole.SNAPSHOT, AnnotationSetSource.CURATOR,
                null, null, "alice", null, null, null,
                "{\"frozen\":\"a\"}", null );
        AnnotationSetService.AttachedAnnotationSet b = annotationSetService.attach(
                preboarded, AnnotationSetRole.SNAPSHOT, AnnotationSetSource.CURATOR,
                null, null, "alice", null, null, null,
                "{\"frozen\":\"b\"}", null );
        assertTrue( a.isCreated() );
        assertTrue( b.isCreated() );
        assertNotNull( a.getAnnotationSet().getId() );
        assertNotNull( b.getAnnotationSet().getId() );
        // Distinct rows even with same createdBy — UUID runId differs per call.
        assertTrue( !a.getAnnotationSet().getId().equals( b.getAnnotationSet().getId() ),
                "two SNAPSHOT attaches should yield distinct rows" );
    }

    @Test
    @DisplayName("finalize / reopen flip finalizedAt idempotently")
    public void finalize_reopen() {
        AnnotationSet draft = annotationSetService.upsertDraft(
                preboarded, "alice", "{\"v\":1}", null, null );
        Long id = draft.getId();
        assertNull( draft.getFinalizedAt() );

        AnnotationSet finalized = annotationSetService.finalizeSet( id, "alice", "  looks good  " );
        assertNotNull( finalized );
        assertNotNull( finalized.getFinalizedAt() );
        assertEquals( "alice", finalized.getFinalizedBy() );
        assertEquals( "looks good", finalized.getFinalizedNotes(), "the note is trimmed on the way in" );

        // Idempotent: second finalize keeps the same finalizedAt instance.
        java.util.Date originalStamp = finalized.getFinalizedAt();
        AnnotationSet again = annotationSetService.finalizeSet( id, "alice", null );
        assertNotNull( again );
        assertSame( originalStamp, again.getFinalizedAt(),
                "re-finalize on already-finalized row should be a no-op" );
        assertEquals( "looks good", again.getFinalizedNotes(),
                "a note-less re-finalize must not erase the note" );

        // ... except for the note, which is recorded rather than dropped: answering 200 to a
        // discarded sentence is indistinguishable from storing it.
        AnnotationSet renoted = annotationSetService.finalizeSet( id, "alice", "on reflection, ok" );
        assertNotNull( renoted );
        assertSame( originalStamp, renoted.getFinalizedAt(),
                "a note on an already-finalized row must not restamp when it was decided" );
        assertEquals( "on reflection, ok", renoted.getFinalizedNotes() );

        AnnotationSet reopened = annotationSetService.reopenSet( id );
        assertNotNull( reopened );
        assertNull( reopened.getFinalizedAt() );
        assertNull( reopened.getFinalizedBy() );
        assertNull( reopened.getFinalizedNotes(),
                "the note explains one closure and must not survive into the next" );
    }

    @Test
    @DisplayName("findSummariesByInvestigation populates every projected field")
    public void summariesProjection() {
        annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, "run-summary", "agent-1",
                new AnnotationSetService.RunProvenance( "1.0", "claude-x", "4d8fdbc", "cell_type", null ),
                "{\"payload\":\"long enough\"}", null );
        annotationSetService.upsertDraft(
                preboarded, "alice", "{\"draft\":1}", null, null );
        flushAndClear();

        List<AnnotationSetSummaryValueObject> all =
                annotationSetService.findSummariesByInvestigation( preboarded, null );
        assertEquals( 2, all.size() );

        List<AnnotationSetSummaryValueObject> onlyProposals =
                annotationSetService.findSummariesByInvestigation(
                        preboarded, AnnotationSetRole.PROPOSAL );
        assertEquals( 1, onlyProposals.size() );
        AnnotationSetSummaryValueObject sum = onlyProposals.get( 0 );
        assertEquals( AnnotationSetRole.PROPOSAL, sum.getRole() );
        assertEquals( AnnotationSetSource.AGENT, sum.getSource() );
        assertEquals( AgentCurationKind.PROPOSAL, sum.getKind() );
        assertEquals( "run-summary", sum.getRunId() );
        assertEquals( "agent-1", sum.getCreatedBy() );
        assertEquals( "1.0", sum.getAgentVersion() );
        assertEquals( "claude-x", sum.getModel() );
        // The two fields that identify the run's build and specialist must survive into the thin
        // projection: the role=commit listing is the "which agent, from which build" query, and it
        // must not need an N+1 into the full payload endpoint to answer it.
        assertEquals( "4d8fdbc", sum.getRunSha() );
        assertEquals( "cell_type", sum.getAgentName() );
        assertEquals( preboarded.getId(), sum.getInvestigationId() );
        assertNotNull( sum.getPayloadSize(),
                "payloadSize projection should be non-null when payloadJson is non-null" );
        assertTrue( sum.getPayloadSize() > 0 );
    }

    @Test
    @DisplayName("countByRoleSince groups the three roles separately")
    public void countByRole() {
        annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, "run-prop", "agent-1",
                null, null, null, "{}", null );
        annotationSetService.upsertDraft( preboarded, "alice", "{}", null, null );
        annotationSetService.attach(
                preboarded, AnnotationSetRole.SNAPSHOT, AnnotationSetSource.CURATOR,
                null, null, "alice", null, null, null, "{}", null );
        flushAndClear();

        Map<AnnotationSetRole, Long> byRole = annotationSetService.countByRoleSince( null );
        assertTrue( byRole.getOrDefault( AnnotationSetRole.PROPOSAL, 0L ) >= 1L );
        assertTrue( byRole.getOrDefault( AnnotationSetRole.DRAFT, 0L ) >= 1L );
        assertTrue( byRole.getOrDefault( AnnotationSetRole.SNAPSHOT, 0L ) >= 1L );
    }

    @Test
    @DisplayName("run provenance (sha + agent name) survives a persist/reload")
    public void runProvenance_roundTrips() {
        String runId = "run-" + UUID.randomUUID();
        AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, runId, "agent-1",
                new AnnotationSetService.RunProvenance( "0.9.0", "claude-sonnet-5", "4d8fdbc", "cell_type", null ),
                "{\"factors\":[]}", null );
        assertTrue( attached.isCreated() );
        Long id = attached.getAnnotationSet().getId();
        flushAndClear();

        AnnotationSet reloaded = annotationSetService.load( id );
        assertNotNull( reloaded );
        // The sha is what identifies the build; the model alone does not, which is why both are stored.
        assertEquals( "4d8fdbc", reloaded.getRunSha() );
        assertEquals( "cell_type", reloaded.getAgentName() );
        assertEquals( "claude-sonnet-5", reloaded.getModel() );
        assertEquals( "0.9.0", reloaded.getAgentVersion() );
    }

    @Test
    @DisplayName("the pre-provenance attach overload still works and leaves the new columns null")
    public void legacyAttachOverload_leavesRunProvenanceNull() {
        String runId = "run-" + UUID.randomUUID();
        AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, runId, "agent-1",
                "0.8.0", "claude-opus-4-7", null,
                "{\"factors\":[]}", null );
        Long id = attached.getAnnotationSet().getId();
        flushAndClear();

        AnnotationSet reloaded = annotationSetService.load( id );
        assertNotNull( reloaded );
        assertEquals( "0.8.0", reloaded.getAgentVersion(), "the old overload still records what it always did" );
        // Null means "not recorded", never "none" — a producer that predates run provenance says nothing.
        assertNull( reloaded.getRunSha() );
        assertNull( reloaded.getAgentName() );
    }

    /**
     * The reason the route exists: correcting a mis-stamped envelope used to mean delete + recreate,
     * which mints a new id, and set ids are quoted across handoffs. So the id staying put is the
     * assertion that matters, alongside the untouched fields.
     */
    @Test
    @DisplayName("updateProvenance corrects the envelope in place, keeps the id, and leaves content alone")
    public void updateProvenance_correctsInPlace() {
        String runId = "run-" + UUID.randomUUID();
        AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, runId, "agent-1",
                new AnnotationSetService.RunProvenance( "0.9.0", "claude-sonnet-5", "4d8fdbc", "cell_type", null ),
                "{\"factors\":[]}", null );
        Long id = attached.getAnnotationSet().getId();
        flushAndClear();

        // Correct the model only; every other field is omitted and must survive.
        AnnotationSet updated = annotationSetService.updateProvenance( id,
                new AnnotationSetService.RunProvenance( null, "claude-opus-5", null, null, null ) );
        assertNotNull( updated );
        assertEquals( id, updated.getId(), "correcting the envelope must not mint a new id" );
        flushAndClear();

        AnnotationSet reloaded = annotationSetService.load( id );
        assertNotNull( reloaded );
        assertEquals( "claude-opus-5", reloaded.getModel() );
        assertEquals( "0.9.0", reloaded.getAgentVersion(), "an omitted field is left alone" );
        assertEquals( "4d8fdbc", reloaded.getRunSha() );
        assertEquals( "cell_type", reloaded.getAgentName() );
        // Envelope only: content and identity are not reachable through this call.
        assertEquals( "{\"factors\":[]}", reloaded.getPayloadJson() );
        assertEquals( runId, reloaded.getRunId() );
        assertEquals( AnnotationSetRole.PROPOSAL, reloaded.getRole() );
    }

    @Test
    @DisplayName("a blank string clears an envelope field, which null cannot express")
    public void updateProvenance_blankClears() {
        String runId = "run-" + UUID.randomUUID();
        Long id = annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, runId, "agent-1",
                new AnnotationSetService.RunProvenance( "0.9.0", "claude-sonnet-5", "4d8fdbc", "cell_type", null ),
                "{}", null ).getAnnotationSet().getId();
        flushAndClear();

        annotationSetService.updateProvenance( id,
                new AnnotationSetService.RunProvenance( null, null, "  ", null, null ) );
        flushAndClear();

        AnnotationSet reloaded = annotationSetService.load( id );
        assertNotNull( reloaded );
        assertNull( reloaded.getRunSha(), "a blank sha means the caller is retracting it" );
        assertEquals( "cell_type", reloaded.getAgentName() );
    }

    @Test
    @DisplayName("updateProvenance on an id that does not exist returns null, not an exception")
    public void updateProvenance_unknownId() {
        assertNull( annotationSetService.updateProvenance( -1L,
                new AnnotationSetService.RunProvenance( "0.1.0", null, null, null, null ) ) );
    }

    /**
     * A queue is ordered by when the RUN happened, not by when the row was stored, and the two
     * disagree: a set carrying August's run can be written today. Ordering by `ranAt` was hardcoded
     * to `createdAt desc` until cab asked for the queue view.
     * <p>
     * 🛑 Runs on real MySQL deliberately. The claim that a null `ranAt` sorts LAST under `desc` is
     * MySQL's null-ordering rule; H2 is emulation and is not evidence for it.
     */
    @Test
    @DisplayName("a proposal starts pending; a role that is not reviewed has no status at all")
    public void status_defaultsToPendingOnProposalsOnly() {
        AnnotationSet proposal = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-st-" + UUID.randomUUID(),
                "agent-status", null, null, null, "{}", null ).getAnnotationSet();
        AnnotationSet commit = annotationSetService.attach( preboarded, AnnotationSetRole.COMMIT,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-st2-" + UUID.randomUUID(),
                "agent-status", null, null, null, "{}", null ).getAnnotationSet();
        flushAndClear();

        assertEquals( "pending", annotationSetService.load( proposal.getId() ).getStatus(),
                "lowercase, the spelling the column and the wire share" );
        // 🛑 Not "pending": null here means the set is not a kind that gets reviewed. Collapsing the
        // two would make a commit look like outstanding review work in any status-filtered queue.
        assertNull( annotationSetService.load( commit.getId() ).getStatus(),
                "a commit is not a thing anybody rules on" );
    }

    @Test
    @DisplayName("any status value is stored -- the vocabulary is open on purpose")
    public void status_isNotAClosedSet() {
        AnnotationSet a = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-open-" + UUID.randomUUID(),
                "agent-status", null, null, null, "{}", null ).getAnnotationSet();
        // A fifth value nobody has agreed on. Paul, 2026-09-04: "don't lock us into any kind of enums."
        // If this ever throws, someone has re-closed the vocabulary.
        annotationSetService.updateStatus( a, "escalated_to_paul" );
        flushAndClear();
        assertEquals( "escalated_to_paul", annotationSetService.load( a.getId() ).getStatus() );
    }

    @Test
    @DisplayName("counts come off the payload, and are refreshed when the payload is rewritten")
    public void counts_areDerivedAndStayInStepWithThePayload() {
        String two = "{\"design\":{\"factors\":{\"items\":[{},{}]}},\"tags\":{\"items\":[{}]}}";
        AnnotationSet draft = annotationSetService.upsertDraft( preboarded, "curator-counts", two, null, null );
        flushAndClear();
        AnnotationSet reloaded = annotationSetService.load( draft.getId() );
        assertEquals( Integer.valueOf( 2 ), reloaded.getFactorCount() );
        assertEquals( Integer.valueOf( 1 ), reloaded.getTagCount() );

        // The draft upsert rewrites the payload of a row that already carries counts. If the two ever
        // drift, a card describes the previous revision while claiming to describe this one.
        String none = "{\"design\":{\"factors\":{\"items\":[]}},\"tags\":{\"items\":[]}}";
        annotationSetService.upsertDraft( preboarded, "curator-counts", none, null, null );
        flushAndClear();
        AnnotationSet after = annotationSetService.load( draft.getId() );
        assertEquals( Integer.valueOf( 0 ), after.getFactorCount(), "emptied, not stale" );
        assertEquals( Integer.valueOf( 0 ), after.getTagCount() );

        // A shape Gemma cannot read reports unknown rather than zero.
        annotationSetService.upsertDraft( preboarded, "curator-counts", "{\"audit_proposal\":{}}", null, null );
        flushAndClear();
        assertNull( annotationSetService.load( draft.getId() ).getFactorCount() );
    }

    @Test
    @DisplayName("kind separates audit output from proposals, which role alone does not")
    public void listSummaries_filtersByKindAndStatus() {
        String createdBy = "agent-kind-" + UUID.randomUUID();
        annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-k1-" + UUID.randomUUID(),
                createdBy, null, null, null, "{}", null );
        // 🛑 role=PROPOSAL, kind=AUDIT. Not a contrivance: 6 of the 8 role=proposal rows on gemma2 are
        // exactly this (cab, 2026-09-04), so a queue filtering role alone lists audit findings as
        // proposals. This is the row that makes the kind filter earn its place.
        annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.AUDIT, "run-k2-" + UUID.randomUUID(),
                createdBy, null, null, null, "{}", null );
        flushAndClear();
        List<Long> invIds = Collections.singletonList( preboarded.getId() );

        assertEquals( 2, annotationSetService.countSummaries( AnnotationSetRole.PROPOSAL, null,
                createdBy, null, null, invIds ), "role alone does not separate them" );
        assertEquals( 1, annotationSetService.countSummaries( AnnotationSetRole.PROPOSAL, null,
                createdBy, AgentCurationKind.PROPOSAL, null, invIds ), "kind does" );
        assertEquals( 1, annotationSetService.listSummaries( AnnotationSetRole.PROPOSAL, null,
                createdBy, AgentCurationKind.AUDIT, null, invIds, 0, 10, null, true ).size() );

        // status filter, and the count must agree with the page or a caller cannot page to the total
        assertEquals( 2, annotationSetService.countSummaries( AnnotationSetRole.PROPOSAL, null,
                createdBy, null, "pending", invIds ) );
        assertEquals( 0, annotationSetService.countSummaries( AnnotationSetRole.PROPOSAL, null,
                createdBy, null, "accepted", invIds ) );
    }

    @Test
    @DisplayName("a set on a non-experiment investigation still lists, with no short name")
    public void listSummaries_leftJoinKeepsNonExperimentRows() {
        String createdBy = "agent-join-" + UUID.randomUUID();
        annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-j-" + UUID.randomUUID(),
                createdBy, null, null, null, "{}", null );
        flushAndClear();

        // 🛑 The guard on the datasetShortName join. shortName lives on ExpressionExperiment, so
        // reaching it needs a downcast; an INNER join would drop every row whose investigation is some
        // other Investigation subtype -- this preboarded one -- and the list would simply be short, with
        // nothing to indicate it. The assertion that matters is the size, not the null.
        List<AnnotationSetSummaryValueObject> rows = annotationSetService.listSummaries(
                AnnotationSetRole.PROPOSAL, null, createdBy, null, null,
                Collections.singletonList( preboarded.getId() ), 0, 10, null, true );
        assertEquals( 1, rows.size(), "the downcast must not filter" );
        assertNull( rows.get( 0 ).getDatasetShortName(), "and it has no short name to report" );
    }

    @Test
    @DisplayName("Cross-experiment list sorts by ranAt, nulls last, independent of createdAt")
    public void listSummaries_sortsByRanAt() {
        Calendar cal = Calendar.getInstance();
        cal.set( 2026, Calendar.JUNE, 1, 0, 0, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        Date june = cal.getTime();
        cal.set( 2026, Calendar.AUGUST, 1, 0, 0, 0 );
        Date august = cal.getTime();

        // stored oldest-run-first, so createdAt order is the REVERSE of ranAt order and a test that
        // passed on either ordering cannot pass on both
        Long juneId = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-june-" + UUID.randomUUID(),
                "agent-sort", null, null, june, "{}", null ).getAnnotationSet().getId();
        Long augustId = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL, "run-aug-" + UUID.randomUUID(),
                "agent-sort", null, null, august, "{}", null ).getAnnotationSet().getId();
        // 🛑 CURATOR, not AGENT: attach stamps ranAt = now on any AGENT set that does not supply one,
        // so an agent set is never null here and only a non-agent set exercises the null ordering
        Long noRunId = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.CURATOR, AgentCurationKind.PROPOSAL, "run-none-" + UUID.randomUUID(),
                "agent-sort", null, null, null, "{}", null ).getAnnotationSet().getId();
        flushAndClear();

        List<Long> invIds = Collections.singletonList( preboarded.getId() );

        List<Long> byRanAt = annotationSetService
                .listSummaries( AnnotationSetRole.PROPOSAL, null, "agent-sort", null, null, invIds, 0, 10,
                        AnnotationSetDao.SummarySort.RAN_AT, true )
                .stream().map( AnnotationSetSummaryValueObject::getId ).collect( Collectors.toList() );
        assertEquals( Arrays.asList( augustId, juneId, noRunId ), byRanAt,
                "newest run first, and the set no run produced sorts last" );

        List<Long> byCreatedAt = annotationSetService
                .listSummaries( AnnotationSetRole.PROPOSAL, null, "agent-sort", null, null, invIds, 0, 10,
                        AnnotationSetDao.SummarySort.CREATED_AT, true )
                .stream().map( AnnotationSetSummaryValueObject::getId ).collect( Collectors.toList() );
        assertEquals( noRunId, byCreatedAt.get( 0 ),
                "createdAt order is the storage order, which is not the run order" );

        List<Long> ascending = annotationSetService
                .listSummaries( AnnotationSetRole.PROPOSAL, null, "agent-sort", null, null, invIds, 0, 10,
                        AnnotationSetDao.SummarySort.RAN_AT, false )
                .stream().map( AnnotationSetSummaryValueObject::getId ).collect( Collectors.toList() );
        assertEquals( Arrays.asList( noRunId, juneId, augustId ), ascending,
                "ascending reverses it, nulls first" );
    }
}
