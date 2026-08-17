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

        AnnotationSet finalized = annotationSetService.finalizeSet( id, "alice" );
        assertNotNull( finalized );
        assertNotNull( finalized.getFinalizedAt() );
        assertEquals( "alice", finalized.getFinalizedBy() );

        // Idempotent: second finalize keeps the same finalizedAt instance.
        java.util.Date originalStamp = finalized.getFinalizedAt();
        AnnotationSet again = annotationSetService.finalizeSet( id, "alice" );
        assertNotNull( again );
        assertSame( originalStamp, again.getFinalizedAt(),
                "re-finalize on already-finalized row should be a no-op" );

        AnnotationSet reopened = annotationSetService.reopenSet( id );
        assertNotNull( reopened );
        assertNull( reopened.getFinalizedAt() );
        assertNull( reopened.getFinalizedBy() );
    }

    @Test
    @DisplayName("findSummariesByInvestigation populates every projected field")
    public void summariesProjection() {
        annotationSetService.attach(
                preboarded, AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT,
                AgentCurationKind.PROPOSAL, "run-summary", "agent-1",
                "1.0", "claude-x", null,
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
}
