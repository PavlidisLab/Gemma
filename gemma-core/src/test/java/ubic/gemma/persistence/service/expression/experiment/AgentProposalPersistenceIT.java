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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link AgentProposalService} against the real MySQL
 * gemdtest. Exercises the audit / proposal lifecycle the curation-UI and CAB
 * agents use:
 *
 * <ul>
 *   <li>{@code attach} — create + idempotency by {@code (investigation, kind, runId)}</li>
 *   <li>{@code findByInvestigation} / {@code findSummariesByInvestigation} — list</li>
 *   <li>{@code updateDisposition} — curator marks ACKNOWLEDGED / FIXED / WONTFIX / etc.</li>
 *   <li>{@code finalizeProposal} → status FINALIZED + {@code finalizedAt} set</li>
 *   <li>{@code reopenProposal} → status REOPENED + {@code finalizedAt} cleared</li>
 *   <li>JSON payload round-trip through MySQL LONGTEXT (CAB ships JSON; server treats
 *       it as opaque)</li>
 * </ul>
 *
 * <p>Uses {@link PreboardedExperiment} as the investigation target — minimal entity
 * (accession + source + identifying metadata), no need to build an EE fixture chain.
 * The proposal entity contract is the same regardless of which Investigation subclass
 * it attaches to.</p>
 *
 * <p>Class-level {@link Transactional} gives each test method its own auto-rolling-back
 * transaction. The nested {@code DetachedEntityRegression} class uses
 * {@link TransactionTemplate} to commit + close BETWEEN persist and read, simulating
 * the JAX-RS request boundary (same pattern that caught the ticket lazy-init bugs).</p>
 */
@Transactional
public class AgentProposalPersistenceIT extends BaseIntegrationTest5 {

    /**
     * Compact CAB-shaped audit payload. The real curation-agents fixture at
     * {@code gemma-curation-agents/.../audit/fixtures/sample_audit_report.json} is
     * 8.3 kB / 210 lines; this trims it to a representative subset that still
     * exercises every nested-object kind (top-level metadata, scope, findings
     * array of mixed target_kind, severity, citation, suggested_fix). The server
     * does NOT validate the JSON shape — payload_json is an opaque LONGTEXT — so
     * what matters here is that arbitrary CAB-shaped content round-trips through
     * Hibernate/MySQL without truncation or escaping mangling.
     */
    private static final String CAB_AUDIT_PAYLOAD = "{"
            + "\"audit_id\":\"aud_01HXYZ_test\","
            + "\"experiment_id\":12345,"
            + "\"audited_at\":\"2026-05-27T19:00:00Z\","
            + "\"model\":\"claude-opus-4-7\","
            + "\"scope\":{\"include\":[\"factors\",\"fvs\",\"tags\"]},"
            + "\"findings\":["
            + "{\"target_kind\":\"experiment\",\"target_id\":\"experiment:12345\","
            + "\"severity\":\"blocker\",\"issue_code\":\"not_suitable_for_dea\","
            + "\"rationale\":\"Single-condition time course; no comparator arm.\","
            + "\"suggested_fix\":\"Mark not-suitable-for-DEA.\"},"
            + "{\"target_kind\":\"factor\",\"target_id\":\"factor:411\","
            + "\"severity\":\"major\",\"issue_code\":\"forbidden_efc\","
            + "\"rationale\":\"'dose' is forbidden as top-level EFC.\","
            + "\"suggested_fix\":\"Replace with treatment factor + has_dose predicate.\"}"
            + "]}";

    @Autowired
    private AgentProposalService agentProposalService;

    @Autowired
    private PreboardedExperimentService preboardedExperimentService;

    @Autowired
    private SessionFactory sessionFactory;

    private PreboardedExperiment investigation;

    @BeforeEach
    public void seedInvestigation() throws PreboardedExperimentService.AccessionAlreadyExistsException {
        // Unique accession per test run; concurrent invocations don't collide.
        String accession = "GSE" + Math.abs( UUID.randomUUID().hashCode() );
        investigation = preboardedExperimentService.createPreboarded(
                accession, "GEO", "{\"title\":\"IT-fixture preboarded\"}" );
        assertNotNull( investigation.getId() );
    }

    private void flushAndClear() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    @Test
    @DisplayName("attach: creates a new AUDIT proposal; status defaults to OPEN, payload round-trips through MySQL LONGTEXT")
    public void attach_createsAudit_withPayload() {
        String runId = "run_" + UUID.randomUUID();
        Date ranAt = new Date();

        AgentProposalService.AttachedProposal result = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT, runId,
                "agent-v1.2.3", "claude-opus-4-7", ranAt, CAB_AUDIT_PAYLOAD );

        assertTrue( result.isCreated(), "fresh attach should report created=true" );
        AgentProposal p = result.getProposal();
        assertNotNull( p.getId(), "id assigned by persist" );
        assertEquals( AgentCurationKind.AUDIT, p.getKind() );
        assertEquals( runId, p.getRunId() );
        assertEquals( "agent-v1.2.3", p.getAgentVersion() );
        assertEquals( "claude-opus-4-7", p.getModel() );
        assertEquals( "OPEN", p.getStatus() );
        assertNull( p.getDisposition() );
        assertNull( p.getFinalizedAt() );

        flushAndClear();
        AgentProposal reloaded = agentProposalService.load( p.getId() );
        assertNotNull( reloaded );
        assertEquals( CAB_AUDIT_PAYLOAD, reloaded.getPayloadJson(),
                "payload bytes must survive the MySQL LONGTEXT round-trip without escaping mangling" );
        assertEquals( investigation.getId(), reloaded.getInvestigation().getId() );
    }

    @Test
    @DisplayName("attach: (investigation, kind, runId) idempotency — replay returns the existing row")
    public void attach_idempotency_replayReturnsExisting() {
        String runId = "run_" + UUID.randomUUID();
        AgentProposalService.AttachedProposal first = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT, runId, null, null, null, "{}" );
        assertTrue( first.isCreated() );
        Long firstId = first.getProposal().getId();

        // Replay with the SAME (investigation, kind, runId) — should return the same row.
        AgentProposalService.AttachedProposal replay = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT, runId,
                "different-agent-version-ignored",
                "different-model-ignored",
                new Date(), "{\"different\":\"payload\"}" );

        assertFalse( replay.isCreated(), "replay should report created=false" );
        assertEquals( firstId, replay.getProposal().getId(),
                "replay should return the same row id" );
    }

    @Test
    @DisplayName("attach: same runId with different kind creates a SEPARATE row (proposal vs audit coexist)")
    public void attach_kindIsPartOfIdempotencyKey() {
        String runId = "run_" + UUID.randomUUID();
        AgentProposalService.AttachedProposal proposal = agentProposalService.attach(
                investigation, AgentCurationKind.PROPOSAL, runId, null, null, null, "{}" );
        AgentProposalService.AttachedProposal audit = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT, runId, null, null, null, "{}" );

        assertTrue( proposal.isCreated() );
        assertTrue( audit.isCreated(), "audit with same runId is a separate row" );
        assertEquals( 2L, agentProposalService.countByInvestigation( investigation ) );
    }

    @Test
    @DisplayName("updateDisposition: sets disposition + note, bumps lastUpdated")
    public void updateDisposition_setsFieldsAndBumpsTimestamp() {
        AgentProposalService.AttachedProposal r = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT,
                "run_" + UUID.randomUUID(), null, null, null, CAB_AUDIT_PAYLOAD );
        Long id = r.getProposal().getId();
        // lastUpdated may be null on a freshly-attached row (updateDisposition is the
        // first event that sets it). Capture pre-state so we can verify it lands.
        Date initialLastUpdated = r.getProposal().getLastUpdated();
        flushAndClear();

        AgentProposal updated = agentProposalService.updateDisposition(
                id, "ACKNOWLEDGED", "Curator reviewed; will action factor:411 in next pass." );
        flushAndClear();

        AgentProposal reloaded = agentProposalService.load( id );
        assertEquals( "ACKNOWLEDGED", reloaded.getDisposition() );
        assertEquals( "Curator reviewed; will action factor:411 in next pass.",
                reloaded.getDispositionNote() );
        assertNotNull( reloaded.getLastUpdated(),
                "lastUpdated should be set after a disposition change" );
        if ( initialLastUpdated != null ) {
            assertTrue( reloaded.getLastUpdated().getTime() >= initialLastUpdated.getTime(),
                    "lastUpdated should not move backwards" );
        }
        // Disposition change alone does NOT finalize — status stays OPEN.
        assertEquals( "OPEN", reloaded.getStatus() );
        assertNull( reloaded.getFinalizedAt() );
    }

    @Test
    @DisplayName("finalizeProposal: status FINALIZED + finalizedAt set")
    public void finalize_sets_FINALIZED_and_finalizedAt() {
        AgentProposalService.AttachedProposal r = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT,
                "run_" + UUID.randomUUID(), null, null, null, "{}" );
        Long id = r.getProposal().getId();
        flushAndClear();

        agentProposalService.finalizeProposal( id );
        flushAndClear();

        AgentProposal reloaded = agentProposalService.load( id );
        assertEquals( "FINALIZED", reloaded.getStatus() );
        assertNotNull( reloaded.getFinalizedAt(), "finalizedAt should be set" );
    }

    @Test
    @DisplayName("reopenProposal: takes a FINALIZED proposal back to REOPENED + clears finalizedAt")
    public void reopen_clears_finalizedAt() {
        AgentProposalService.AttachedProposal r = agentProposalService.attach(
                investigation, AgentCurationKind.AUDIT,
                "run_" + UUID.randomUUID(), null, null, null, "{}" );
        Long id = r.getProposal().getId();
        agentProposalService.finalizeProposal( id );
        flushAndClear();

        agentProposalService.reopenProposal( id );
        flushAndClear();

        AgentProposal reloaded = agentProposalService.load( id );
        assertEquals( "REOPENED", reloaded.getStatus() );
        assertNull( reloaded.getFinalizedAt(), "reopen should clear finalizedAt" );
    }

    @Test
    @DisplayName("findSummariesByInvestigation: returns thin metadata (no payload_json) with payloadSize")
    public void findSummaries_returnsThinProjection() {
        agentProposalService.attach( investigation, AgentCurationKind.AUDIT,
                "run_a", "agent-v1", "claude-opus-4-7", new Date(), CAB_AUDIT_PAYLOAD );
        agentProposalService.attach( investigation, AgentCurationKind.PROPOSAL,
                "run_b", "agent-v1", "claude-opus-4-7", new Date(), "{}" );
        flushAndClear();

        List<AgentCurationSummaryValueObject> all = agentProposalService.findSummariesByInvestigation(
                investigation, null );
        assertEquals( 2, all.size(), "both kinds returned when filter is null" );

        List<AgentCurationSummaryValueObject> audits = agentProposalService.findSummariesByInvestigation(
                investigation, AgentCurationKind.AUDIT );
        assertEquals( 1, audits.size(), "kind filter restricts to AUDIT only" );

        // The thin projection should carry payloadSize (bytes) so the UI can decide whether to fetch full.
        Long payloadSize = audits.get( 0 ).getPayloadSize();
        assertNotNull( payloadSize );
        assertTrue( payloadSize > 0 );
    }

    /**
     * Regression coverage for the JAX-RS lazy-init pattern. {@link AgentProposal} has
     * a LAZY {@code investigation} {@code @ManyToOne} — the same shape that bit the
     * Ticket layer's reporter Contact. Uses {@link TransactionTemplate} to commit +
     * close the persist transaction BEFORE the read, simulating a JAX-RS handler that
     * holds a detached entity reference and tries to access lazy fields.
     */
    @Nested
    @DisplayName("Detached-entity / lazy-init regression (JAX-RS boundary)")
    class DetachedEntityRegression {

        @Autowired
        private PlatformTransactionManager txManager;

        @Test
        @DisplayName("load + access investigation works across txn boundary when service force-initialises")
        public void load_thenAccessInvestigation_acrossTransactions() {
            TransactionTemplate tx = new TransactionTemplate( txManager );
            Long id = tx.execute( status -> {
                AgentProposalService.AttachedProposal r = agentProposalService.attach(
                        investigation, AgentCurationKind.AUDIT,
                        "run_" + UUID.randomUUID(), null, null, null, CAB_AUDIT_PAYLOAD );
                return r.getProposal().getId();
            } );

            // Reload + initialise investigation inside a fresh txn — simulates the read-side
            // service contract a future VO-loading method would need to honour.
            tx.executeWithoutResult( status -> {
                AgentProposal reloaded = agentProposalService.load( id );
                assertNotNull( reloaded );
                Investigation inv = reloaded.getInvestigation();
                Hibernate.initialize( inv );
                assertNotNull( inv.getId() );
                assertTrue( inv instanceof PreboardedExperiment,
                        "investigation should still type-resolve to its concrete subclass" );
            } );
        }

        @Test
        @DisplayName("finalize on a row loaded in a previous txn doesn't break ('attached' semantics)")
        public void finalize_afterPriorLoad() {
            TransactionTemplate tx = new TransactionTemplate( txManager );
            Long id = tx.execute( status -> {
                AgentProposalService.AttachedProposal r = agentProposalService.attach(
                        investigation, AgentCurationKind.AUDIT,
                        "run_" + UUID.randomUUID(), null, null, null, "{}" );
                return r.getProposal().getId();
            } );
            // Load in one txn, finalize in another — the service's @Transactional on
            // finalizeProposal opens its own session; the id arg means it doesn't have
            // to inherit a detached entity.
            tx.executeWithoutResult( status -> {
                AgentProposal _loaded = agentProposalService.load( id );
                assertNotNull( _loaded );
            } );

            agentProposalService.finalizeProposal( id );

            tx.executeWithoutResult( status -> {
                AgentProposal reloaded = agentProposalService.load( id );
                assertEquals( "FINALIZED", reloaded.getStatus() );
            } );
        }
    }
}
