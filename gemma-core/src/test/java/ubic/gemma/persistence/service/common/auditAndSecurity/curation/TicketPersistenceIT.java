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

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.hibernate.LazyInitializationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketMode;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketSearchHitValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TicketAssignedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TicketMetadataChangedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TicketOpenedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TicketStateChangedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TicketTargetStatusChangedEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the V19 migration ({@code TICKET.MODE} + {@code TICKET_TARGET.STATUS})
 * and the corresponding {@link Ticket}/{@link TicketTarget} entity mappings.
 *
 * <p>Runs against the real MySQL {@code gemdtest} via {@link BaseIntegrationTest5} —
 * proves that:</p>
 *
 * <ol>
 *   <li>Flyway migration V19 ({@code ticket_mode_and_target_status.sql}) applies cleanly
 *       on a fresh schema (failure would fail at context init, before any @Test).</li>
 *   <li>The {@code @Column(name="MODE")} and {@code @Column(name="STATUS")} mappings
 *       round-trip through Hibernate against the real MySQL column types
 *       ({@code VARCHAR(16)}).</li>
 *   <li>Default values (MANUAL / NOT_DONE) materialise correctly when not set
 *       explicitly.</li>
 *   <li>The {@code body} convenience alias on {@link Ticket} (which routes to the
 *       inherited {@code description} column) persists end-to-end.</li>
 * </ol>
 *
 * <p>Class-level {@link Transactional} opens a per-test transaction that Spring's
 * {@code TransactionalTestExecutionListener} rolls back at end-of-test, so no
 * persistent cleanup is needed. The transaction also gives all DAO calls a
 * session to attach to (the alternative — calling DAOs from a transaction-less
 * context — fails with "could not obtain transaction-synchronized Session").
 * Between persist and reload assertions we explicitly flush + clear the Hibernate
 * session so the reload exercises a real SQL round-trip rather than reading from
 * the L1 cache.</p>
 *
 * <p>These guarantees can't be exercised by the existing Mockito unit tests
 * ({@code TicketServiceImplTest}, {@code TicketsWebServiceTest},
 * {@code TicketValueObjectTest}) — those skip the schema + Hibernate
 * round-trip entirely.</p>
 */
@Transactional
public class TicketPersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketDao ticketDao;

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SessionFactory sessionFactory;

    private static final com.fasterxml.jackson.databind.ObjectMapper PAYLOAD_TEST_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private Contact reporter;

    @BeforeEach
    public void seedReporter() {
        // @Nested classes inherit this @BeforeEach. ListPathDetachedRegression deliberately runs
        // with the test-managed transaction suspended (@Transactional(NOT_SUPPORTED)), where a bare
        // DAO write fails with "could not obtain transaction-synchronized Session" — it seeds its
        // own committed fixture instead and never reads this field.
        if ( !TransactionSynchronizationManager.isActualTransactionActive() ) {
            return;
        }
        Contact c = new Contact();
        c.setName( "ticket-it-reporter-" + UUID.randomUUID() );
        reporter = contactDao.create( c );
    }

    /**
     * Force a Hibernate flush + L1 cache clear so a subsequent load() goes back to the DB
     * rather than returning the same managed instance. Without this the test would still pass
     * but wouldn't actually verify the persistence round-trip.
     */
    private void flushAndClear() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    @Test
    @DisplayName("default mode=MANUAL + target status=NOT_DONE round-trip through real MySQL")
    public void defaults_roundTrip() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 12345L );
        // status NOT set — entity default + V19 column default should both pick NOT_DONE.

        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "default-fields-roundtrip",
                Collections.singleton( target ) );
        Long id = created.getId();
        assertNotNull( id, "openTicket should have assigned an id" );
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        assertNotNull( reloaded );
        assertEquals( TicketMode.MANUAL, reloaded.getMode(),
                "default mode should round-trip as MANUAL (entity default + V19 column default agree)" );
        assertNull( reloaded.getBody(), "body unset should round-trip as null (description column nullable)" );

        Set<TicketTarget> reloadedTargets = reloaded.getTargets();
        assertEquals( 1, reloadedTargets.size() );
        TicketTarget t0 = reloadedTargets.iterator().next();
        assertEquals( TicketTargetStatus.NOT_DONE, t0.getStatus(),
                "default target status should round-trip as NOT_DONE" );
        assertEquals( TicketTargetType.EXPRESSION_EXPERIMENT, t0.getTargetType() );
        assertEquals( 12345L, t0.getTargetId() );
    }

    @Test
    @DisplayName("payload written through updateMetadata survives -- the reattach copy list used to drop it")
    public void payload_survivesUpdateMetadata() throws Exception {
        // 🛑 The regression. updateMetadata() reattaches the ticket and, when the reattached instance
        // differs from the caller's (which is ALWAYS the case on the POST /tickets path, because the
        // ticket was created in an earlier transaction), copies a hand-listed set of fields across.
        // payload and payloadSchemaVersion were not on that list, so they were dropped -- while the
        // 201 response, built from the caller's own mutated instance, echoed them back as if stored.
        // Reported from the field: "POST returns 201 and echoes both fields back, a GET right after
        // comes back null; every other field on the same request persists."
        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "payload-roundtrip",
                Collections.singleton( TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 4242L ) ) );
        Long id = created.getId();
        assertNotNull( id );
        flushAndClear();

        // Re-load to get a DETACHED instance, mutate it, and go through the same call the REST layer
        // makes. Mutating `created` directly would leave it managed and hide the bug.
        Ticket detached = ticketDao.load( id );
        assertNotNull( detached );
        flushAndClear();
        detached.setPayload( "{\"question\":\"which strain?\"}" );
        detached.setPayloadSchemaVersion( 3 );
        detached.setTitle( "payload-roundtrip-edited" );
        ticketService.updateMetadata( detached, "payload, payloadSchemaVersion, title" );
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        assertNotNull( reloaded );
        // 🛑 Compared as PARSED JSON, not as bytes. PAYLOAD is a MySQL `json` column, so the server
        // parses and re-serializes what it is given: `{"question":"which strain?"}` comes back as
        // `{"question": "which strain?"}`. The payload is preserved as a DOCUMENT and NOT byte for
        // byte, so nothing downstream may hash or diff the raw string and expect stability.
        assertNotNull( reloaded.getPayload(), "payload must survive updateMetadata, not just be echoed back" );
        assertEquals(
                PAYLOAD_TEST_MAPPER.readTree( "{\"question\":\"which strain?\"}" ),
                PAYLOAD_TEST_MAPPER.readTree( reloaded.getPayload() ),
                "payload must survive updateMetadata as an equivalent document" );
        assertEquals( Integer.valueOf( 3 ), reloaded.getPayloadSchemaVersion(),
                "payloadSchemaVersion must survive too" );
        assertEquals( "payload-roundtrip-edited", reloaded.getTitle(),
                "title was already on the copy list and is the control -- if this fails the test is wrong, not the fix" );
    }

    @Test
    @DisplayName("explicit mode=AUTO, body, target status=UNDERWAY round-trip")
    public void explicitFields_roundTrip() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 6789L );
        target.setStatus( TicketTargetStatus.UNDERWAY );

        Ticket created = ticketService.openTicket(
                reporter, TicketType.PRELOAD, "explicit-fields-roundtrip",
                Collections.singleton( target ) );
        created.setMode( TicketMode.AUTO );
        created.setBody( "Run the preload eutils sweep; expect ~50 samples on this GSE." );
        ticketDao.update( created );
        Long id = created.getId();
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        assertNotNull( reloaded );
        assertEquals( TicketMode.AUTO, reloaded.getMode() );
        assertEquals( "Run the preload eutils sweep; expect ~50 samples on this GSE.", reloaded.getBody() );
        assertEquals( TicketType.PRELOAD, reloaded.getType() );
        assertEquals( TicketState.OPEN, reloaded.getState() );

        Set<TicketTarget> reloadedTargets = reloaded.getTargets();
        assertEquals( 1, reloadedTargets.size() );
        assertEquals( TicketTargetStatus.UNDERWAY, reloadedTargets.iterator().next().getStatus() );
    }

    @Test
    @DisplayName("multi-target ticket: per-target status independently tracked")
    public void multiTarget_perTargetStatusIndependent() {
        TicketTarget tDone = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 1L );
        tDone.setStatus( TicketTargetStatus.DONE );
        TicketTarget tUnderway = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 2L );
        tUnderway.setStatus( TicketTargetStatus.UNDERWAY );
        TicketTarget tNotDone = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 3L );
        // tNotDone leaves the default

        Set<TicketTarget> targets = new HashSet<>();
        targets.add( tDone );
        targets.add( tUnderway );
        targets.add( tNotDone );

        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "multi-target-statuses", targets );
        flushAndClear();

        Ticket reloaded = ticketDao.load( created.getId() );
        assertEquals( 3, reloaded.getTargets().size() );
        // Build a (targetId -> status) view independent of iteration order.
        java.util.Map<Long, TicketTargetStatus> byId = new java.util.HashMap<>();
        for ( TicketTarget t : reloaded.getTargets() ) {
            byId.put( t.getTargetId(), t.getStatus() );
        }
        assertEquals( TicketTargetStatus.DONE, byId.get( 1L ) );
        assertEquals( TicketTargetStatus.UNDERWAY, byId.get( 2L ) );
        assertEquals( TicketTargetStatus.NOT_DONE, byId.get( 3L ) );
    }

    @Test
    @DisplayName("GEO_SCRAPE_WATERMARK target type persists")
    public void geoScrapeWatermarkTarget_persists() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.GEO_SCRAPE_WATERMARK, 42L );
        target.setStatus( TicketTargetStatus.UNDERWAY );

        Ticket created = ticketService.openTicket(
                reporter, TicketType.PRELOAD, "geo-scrape-batch-ticket",
                Collections.singleton( target ) );
        flushAndClear();

        Ticket reloaded = ticketDao.load( created.getId() );
        TicketTarget t = reloaded.getTargets().iterator().next();
        assertEquals( TicketTargetType.GEO_SCRAPE_WATERMARK, t.getTargetType() );
        assertEquals( 42L, t.getTargetId() );
        assertEquals( TicketTargetStatus.UNDERWAY, t.getStatus() );
    }

    @Test
    @DisplayName("body alias actually writes to the DESCRIPTION column (verified via raw SQL)")
    public void body_writesToDescriptionColumn() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 1L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.GENERIC, "body-routes-to-description",
                Collections.singleton( target ) );
        created.setBody( "alias-target-text" );
        ticketDao.update( created );
        Long id = created.getId();
        flushAndClear();

        String descriptionInDb = new JdbcTemplate( dataSource ).queryForObject(
                "SELECT DESCRIPTION FROM TICKET WHERE ID = ?", String.class, id );
        assertEquals( "alias-target-text", descriptionInDb,
                "Body convenience accessor should hit the DESCRIPTION column (inherited from AbstractDescribable)" );
    }

    @Test
    @DisplayName("MODE / STATUS values persist as canonical enum-name strings in MySQL")
    public void modeAndStatus_persistAsEnumNameStrings() {
        // @Enumerated(EnumType.STRING) — the actual string in the DB should be the enum
        // constant name, not its ordinal. A future enum rename would surface here.
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 7L );
        target.setStatus( TicketTargetStatus.DONE );

        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "mode-status-enum-strings",
                Collections.singleton( target ) );
        created.setMode( TicketMode.AUTO );
        ticketDao.update( created );
        Long id = created.getId();
        flushAndClear();

        JdbcTemplate jdbc = new JdbcTemplate( dataSource );
        String modeStr = jdbc.queryForObject(
                "SELECT MODE FROM TICKET WHERE ID = ?", String.class, id );
        assertEquals( "AUTO", modeStr );

        String statusStr = jdbc.queryForObject(
                "SELECT STATUS FROM TICKET_TARGET WHERE TICKET_FK = ?", String.class, id );
        assertEquals( "DONE", statusStr );
    }

    @Test
    @DisplayName("SCREENING round-trips and lands in TYPE as its own name, not truncated")
    public void screeningType_roundTripsAsName() {
        // The TicketType docblock claims new values need no migration because TYPE is
        // VARCHAR(64). SCREENING is the first value added since that claim was written;
        // this pins both halves of it — the value survives the round trip, and the column
        // holds the constant name rather than an ordinal or a truncated string.
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 11L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.SCREENING, "screening-roundtrip",
                Collections.singleton( target ) );
        Long id = created.getId();
        flushAndClear();

        assertEquals( TicketType.SCREENING, ticketDao.load( id ).getType() );
        assertEquals( "SCREENING", new JdbcTemplate( dataSource )
                .queryForObject( "SELECT TYPE FROM TICKET WHERE ID = ?", String.class, id ) );

        // AUDIT is the other value added with no migration; same pin.
        TicketTarget at = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 12L );
        Long aid = ticketService.openTicket( reporter, TicketType.AUDIT, "audit-roundtrip",
                Collections.singleton( at ) ).getId();
        flushAndClear();
        assertEquals( TicketType.AUDIT, ticketDao.load( aid ).getType() );
        assertEquals( "AUDIT", new JdbcTemplate( dataSource )
                .queryForObject( "SELECT TYPE FROM TICKET WHERE ID = ?", String.class, aid ) );
    }

    @Test
    @DisplayName("screeningResult round-trips, is uncoupled from status, and logs its own event")
    public void screeningResult_roundTripsUncoupledAndLogged() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 21L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.SCREENING, "screening-result", Collections.singleton( target ) );
        Long id = created.getId();
        Long targetRowId = created.getTargets().iterator().next().getId();
        flushAndClear();

        // fresh target: no decision recorded
        Ticket loaded = ticketDao.load( id );
        assertNull( loaded.getTargets().iterator().next().getScreeningResult() );
        assertEquals( TicketTargetStatus.NOT_DONE, loaded.getTargets().iterator().next().getStatus() );

        ticketService.updateTargetScreeningResult( loaded, targetRowId, ScreeningResult.REJECT, "superseded by GSE99999", true, reporter );
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        TicketTarget rt = reloaded.getTargets().iterator().next();
        // decision + reason stored...
        assertEquals( ScreeningResult.REJECT, rt.getScreeningResult() );
        assertEquals( "superseded by GSE99999", rt.getScreeningResultReason() );
        // ...as its enum-name string in the column...
        assertEquals( "REJECT", new JdbcTemplate( dataSource )
                .queryForObject( "SELECT SCREENING_RESULT FROM TICKET_TARGET WHERE ID = ?", String.class, targetRowId ) );
        // ...and status is untouched (uncoupled): a REJECT did not force DONE.
        assertEquals( TicketTargetStatus.NOT_DONE, rt.getStatus() );
        // the change is in the ticket event log
        assertTrue( reloaded.getEvents().stream()
                .anyMatch( e -> e.getType() == TicketEventType.SCREENING_RESULT_CHANGED ),
                "expected a SCREENING_RESULT_CHANGED event" );

        // no-op re-set does not append a second event
        int before = reloaded.getEvents().size();
        ticketService.updateTargetScreeningResult( reloaded, targetRowId, ScreeningResult.REJECT, "superseded by GSE99999", true, reporter );
        flushAndClear();
        assertEquals( before, ticketDao.load( id ).getEvents().size(), "no-op re-set should log nothing" );
    }

    @Test
    @DisplayName("screeningResult reason is independent: an absent reason key never wipes the note")
    public void screeningResultReason_absentKeyLeavesReasonUntouched() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 22L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.SCREENING, "reason-independence", Collections.singleton( target ) );
        Long id = created.getId();
        Long rid = created.getTargets().iterator().next().getId();

        // seed a decision + reason
        ticketService.updateTargetScreeningResult( ticketDao.load( id ), rid,
                ScreeningResult.UNDECIDED, "needs the paper", true, reporter );
        flushAndClear();
        int eventsAfterSeed = ticketDao.load( id ).getEvents().size();

        // re-send the SAME decision with NO reason key (reasonProvided=false) -> true no-op:
        // reason preserved, no event appended (the pre-fix bug cleared it and logged a change)
        ticketService.updateTargetScreeningResult( ticketDao.load( id ), rid,
                ScreeningResult.UNDECIDED, null, false, reporter );
        flushAndClear();
        TicketTarget t = ticketDao.load( id ).getTargets().iterator().next();
        assertEquals( ScreeningResult.UNDECIDED, t.getScreeningResult() );
        assertEquals( "needs the paper", t.getScreeningResultReason() );
        assertEquals( eventsAfterSeed, ticketDao.load( id ).getEvents().size(), "no-op must log nothing" );

        // change the decision with NO reason key -> reason survives (client clears it explicitly if wanted)
        ticketService.updateTargetScreeningResult( ticketDao.load( id ), rid,
                ScreeningResult.REJECT, null, false, reporter );
        flushAndClear();
        t = ticketDao.load( id ).getTargets().iterator().next();
        assertEquals( ScreeningResult.REJECT, t.getScreeningResult() );
        assertEquals( "needs the paper", t.getScreeningResultReason() );

        // explicit null reason clears it
        ticketService.updateTargetScreeningResult( ticketDao.load( id ), rid,
                ScreeningResult.REJECT, null, true, reporter );
        flushAndClear();
        assertNull( ticketDao.load( id ).getTargets().iterator().next().getScreeningResultReason() );
    }

    @Test
    @DisplayName("OPEN → IN_PROGRESS → RESOLVED lifecycle: state, updatedAt, and event log all advance")
    public void lifecycle_open_inProgress_resolved() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 99L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.QUALITY_REVIEW, "lifecycle-ticket",
                Collections.singleton( target ) );
        Long id = created.getId();
        Date createdAt = created.getCreatedAt();
        assertNotNull( createdAt );
        assertEquals( TicketState.OPEN, created.getState() );

        ticketService.transition( created, TicketState.IN_PROGRESS, reporter, "starting work" );
        flushAndClear();
        Ticket afterStart = ticketDao.load( id );
        assertEquals( TicketState.IN_PROGRESS, afterStart.getState() );
        assertTrue( afterStart.getUpdatedAt().getTime() >= createdAt.getTime(),
                "updatedAt should advance on a state transition" );

        ticketService.transition( afterStart, TicketState.RESOLVED, reporter, "done" );
        flushAndClear();
        Ticket reloaded = ticketDao.load( id );
        assertEquals( TicketState.RESOLVED, reloaded.getState() );

        // Event log: OPENED + STATE_CHANGED (IN_PROGRESS) + RESOLVED (terminal alias for STATE_CHANGED).
        assertTrue( reloaded.getEvents().size() >= 3,
                "expected at least 3 events (OPENED + 2 transitions), got " + reloaded.getEvents().size() );
        boolean sawOpened = reloaded.getEvents().stream()
                .anyMatch( e -> e.getType() == TicketEventType.OPENED );
        assertTrue( sawOpened, "OPENED event should be in the log" );

        // Governance stream: the inherited AuditTrail should carry the
        // companion typed audit rows in lockstep with the TicketEvent log.
        assertNotNull( reloaded.getAuditTrail(), "auditTrail row must exist for an AbstractAuditable" );
        boolean sawTicketOpenedAudit = reloaded.getAuditTrail().getEvents().stream()
                .anyMatch( e -> e.getEventType() instanceof TicketOpenedEvent );
        boolean sawTicketStateChangedAudit = reloaded.getAuditTrail().getEvents().stream()
                .anyMatch( e -> e.getEventType() instanceof TicketStateChangedEvent );
        assertTrue( sawTicketOpenedAudit, "TicketOpenedEvent should be in the audit trail" );
        assertTrue( sawTicketStateChangedAudit, "TicketStateChangedEvent should be in the audit trail" );
    }

    @Test
    @DisplayName("updateTargetStatus mutates one TicketTarget, appends TARGET_STATUS_CHANGED + audit row")
    public void updateTargetStatus_marksOneTargetDone() {
        TicketTarget tgtA = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 10L );
        TicketTarget tgtB = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 11L );
        Set<TicketTarget> targets = new HashSet<>();
        targets.add( tgtA );
        targets.add( tgtB );

        Ticket created = ticketService.openTicket(
                reporter, TicketType.LITERATURE_SEARCH, "agent-target-status",
                targets );
        Long ticketId = created.getId();
        // Capture the row-id of tgtA so we can drive updateTargetStatus by it.
        Long tgtARowId = created.getTargets().stream()
                .filter( t -> t.getTargetId().equals( 10L ) )
                .findFirst().orElseThrow().getId();
        flushAndClear();

        Ticket toEdit = ticketDao.load( ticketId );
        ticketService.updateTargetStatus( toEdit, tgtARowId, TicketTargetStatus.DONE, reporter );
        flushAndClear();

        Ticket reloaded = ticketDao.load( ticketId );
        // Per-target status independent: only tgtA flipped.
        for ( TicketTarget t : reloaded.getTargets() ) {
            if ( t.getTargetId().equals( 10L ) ) {
                assertEquals( TicketTargetStatus.DONE, t.getStatus(), "tgtA should be DONE" );
            } else {
                assertEquals( TicketTargetStatus.NOT_DONE, t.getStatus(), "tgtB should be untouched" );
            }
        }

        // Workflow stream: TARGET_STATUS_CHANGED is appended (alongside the OPENED).
        boolean sawTargetEvent = reloaded.getEvents().stream()
                .anyMatch( e -> e.getType() == TicketEventType.TARGET_STATUS_CHANGED );
        assertTrue( sawTargetEvent, "TARGET_STATUS_CHANGED should be in the TicketEvent log" );

        // Governance stream: TicketTargetStatusChangedEvent on the audit trail.
        boolean sawTargetAudit = reloaded.getAuditTrail().getEvents().stream()
                .anyMatch( e -> e.getEventType() instanceof TicketTargetStatusChangedEvent );
        assertTrue( sawTargetAudit, "TicketTargetStatusChangedEvent should be in the audit trail" );
        String note = reloaded.getAuditTrail().getEvents().stream()
                .filter( e -> e.getEventType() instanceof TicketTargetStatusChangedEvent )
                .map( e -> e.getNote() )
                .findFirst().orElse( "" );
        assertTrue( note.contains( "NOT_DONE -> DONE" ) && note.contains( "10" ),
                "audit NOTE should describe the change; was: " + note );
    }

    @Test
    @DisplayName("Ticket.events is ordered by occurredAt after reload (List + @OrderBy)")
    public void events_listOrderedByOccurredAt() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 77L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "ordering-test",
                Collections.singleton( target ) );
        Long id = created.getId();
        // Drive several mutations so we have multiple events with distinct timestamps.
        ticketService.transition( created, TicketState.IN_PROGRESS, reporter, "starting" );
        ticketService.addComment( created, reporter, "noted" );
        ticketService.transition( created, TicketState.RESOLVED, reporter, "done" );
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        java.util.List<ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent> evs = reloaded.getEvents();
        assertTrue( evs.size() >= 4, "expected at least 4 events" );
        // @OrderBy("occurredAt") on the List materializes events in ascending time.
        // assertSorted: each event's occurredAt >= the previous one's.
        for ( int i = 1; i < evs.size(); i++ ) {
            assertTrue( !evs.get( i ).getOccurredAt().before( evs.get( i - 1 ).getOccurredAt() ),
                    "events should be sorted ascending by occurredAt; index " + i
                            + " (" + evs.get( i ).getOccurredAt() + ") precedes index "
                            + ( i - 1 ) + " (" + evs.get( i - 1 ).getOccurredAt() + ")" );
        }
        // First event in chronological order must be OPENED.
        assertEquals( TicketEventType.OPENED, evs.get( 0 ).getType() );
    }

    @Test
    @DisplayName("updateMetadata writes TicketMetadataChangedEvent on the audit trail but no TicketEvent")
    public void updateMetadata_auditOnly_noTicketEvent() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 42L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "metadata-edit-it",
                Collections.singleton( target ) );
        Long id = created.getId();
        int eventsAfterOpen = created.getEvents().size();
        flushAndClear();

        Ticket toEdit = ticketDao.load( id );
        toEdit.setPriority( TicketPriority.URGENT );
        toEdit.setBody( "more detail about what to do here" );
        ticketService.updateMetadata( toEdit, "priority, body" );
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        assertEquals( TicketPriority.URGENT, reloaded.getPriority() );
        assertEquals( "more detail about what to do here", reloaded.getBody() );

        // TicketEvent stream MUST NOT grow on metadata edits.
        assertEquals( eventsAfterOpen, reloaded.getEvents().size(),
                "metadata edits must not append a TicketEvent (Decision 4)" );

        // Governance stream DOES grow: TicketMetadataChangedEvent with the
        // changed-fields list in NOTE.
        boolean sawMetadataAudit = reloaded.getAuditTrail().getEvents().stream()
                .anyMatch( e -> e.getEventType() instanceof TicketMetadataChangedEvent );
        assertTrue( sawMetadataAudit, "TicketMetadataChangedEvent should be in the audit trail" );
        String note = reloaded.getAuditTrail().getEvents().stream()
                .filter( e -> e.getEventType() instanceof TicketMetadataChangedEvent )
                .map( e -> e.getNote() )
                .findFirst().orElse( "" );
        assertTrue( note.contains( "priority" ) && note.contains( "body" ),
                "audit NOTE should list the fields that changed; was: " + note );
    }

    @Test
    @DisplayName("multi-target with mixed FACTOR_VALUE + EE persists both target types")
    public void multiTarget_mixedTypes() {
        TicketTarget eeTarget = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 100L );
        TicketTarget fvTarget = TicketTarget.Factory.newInstance( TicketTargetType.FACTOR_VALUE, 200L );
        fvTarget.setStatus( TicketTargetStatus.UNDERWAY );

        Set<TicketTarget> mixed = new HashSet<>();
        mixed.add( eeTarget );
        mixed.add( fvTarget );

        Ticket created = ticketService.openTicket(
                reporter, TicketType.CURATION, "mixed-target-types", mixed );
        flushAndClear();

        Ticket reloaded = ticketDao.load( created.getId() );
        assertEquals( 2, reloaded.getTargets().size() );

        boolean sawEE = false;
        boolean sawFV = false;
        for ( TicketTarget t : reloaded.getTargets() ) {
            if ( t.getTargetType() == TicketTargetType.EXPRESSION_EXPERIMENT ) {
                assertEquals( 100L, t.getTargetId() );
                assertEquals( TicketTargetStatus.NOT_DONE, t.getStatus() );
                sawEE = true;
            } else if ( t.getTargetType() == TicketTargetType.FACTOR_VALUE ) {
                assertEquals( 200L, t.getTargetId() );
                assertEquals( TicketTargetStatus.UNDERWAY, t.getStatus() );
                sawFV = true;
            }
        }
        assertTrue( sawEE && sawFV, "both target types should round-trip" );
    }

    // ---------------------------------------------------------------------
    // GET /tickets/search — the HQL behind the ticket picker, against real MySQL
    // ---------------------------------------------------------------------

    /** Open a ticket with {@code n} distinct EE targets and a title unique to this test run. */

    /**
     * {@code POST /tickets {"type":"SCRATCHPAD"}} answered 500 with the schema in the body —
     * {@code Duplicate entry '1' for key 'TICKET_ONE_SCRATCHPAD_PER_CURATOR'} (cab, 2026-09-01, on
     * gemma2). The rule is right and V40 enforces it; what was missing is a refusal in front of it,
     * so a caller could not tell "you may not do this" from "Gemma is broken" and would retry.
     * <p>
     * The refusal names the ticket that already holds the role, because the caller's next move is to
     * open that one rather than to try again.
     */
    @Test
    @DisplayName("opening a second SCRATCHPAD is refused by the service, not by the constraint")
    public void openTicket_secondScratchpadIsRefusedWithTheExistingId() {
        Ticket pad = ticketService.getOrCreateScratchpad( reporter );
        assertNotNull( pad.getId() );

        Set<TicketTarget> targets = Collections.singleton(
                TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, freshTargetBase() ) );
        IllegalStateException e = assertThrows( IllegalStateException.class,
                () -> ticketService.openTicket( reporter, TicketType.SCRATCHPAD,
                        "second scratchpad " + UUID.randomUUID(), targets ),
                "the service refuses before the insert, so the caller never sees the constraint" );
        assertTrue( e.getMessage().contains( String.valueOf( pad.getId() ) ),
                "the refusal must name the scratchpad that already exists: " + e.getMessage() );

        // an ordinary ticket for the same reporter is unaffected -- the rule is SCRATCHPAD-only
        assertNotNull( ticketService.openTicket( reporter, TicketType.CURATION,
                "ordinary " + UUID.randomUUID(), targets ).getId() );
    }

    private Ticket openWithTargets( TicketType type, String title, int n ) {
        Set<TicketTarget> targets = new HashSet<>();
        for ( int i = 0; i < n; i++ ) {
            targets.add( TicketTarget.Factory.newInstance(
                    TicketTargetType.EXPRESSION_EXPERIMENT, ( long ) ( 1_000_000 + i ) ) );
        }
        return ticketService.openTicket( reporter, type, title, targets );
    }

    @Test
    @DisplayName("search: targetCount is counted by the database, and matches the real target count")
    public void search_targetCountIsCountedNotLoaded() {
        String tag = "searchcount-" + UUID.randomUUID();
        Ticket three = openWithTargets( TicketType.CURATION, tag + " three", 3 );
        Ticket one = openWithTargets( TicketType.CURATION, tag + " one", 1 );
        flushAndClear();

        List<TicketSearchHitValueObject> hits = ticketDao.findSearchHitsByTitle( tag, true, null, 20 );

        assertEquals( 2, hits.size() );
        for ( TicketSearchHitValueObject h : hits ) {
            if ( h.getId().equals( three.getId() ) ) {
                assertEquals( 3L, h.getTargetCount() );
            } else if ( h.getId().equals( one.getId() ) ) {
                assertEquals( 1L, h.getTargetCount() );
            } else {
                throw new AssertionError( "unexpected hit " + h.getId() );
            }
        }
    }

    @Test
    @DisplayName("search: the title matches as a case-insensitive substring")
    public void search_titleMatchesCaseInsensitiveSubstring() {
        String tag = "SearchCase" + UUID.randomUUID().toString().replace( "-", "" );
        Ticket t = openWithTargets( TicketType.CURATION, "Reference 500 — " + tag + " review", 2 );
        flushAndClear();

        // typed in the middle of the title, and in the wrong case
        List<TicketSearchHitValueObject> hits =
                ticketDao.findSearchHitsByTitle( tag.toLowerCase(), true, null, 20 );

        assertEquals( 1, hits.size() );
        assertEquals( t.getId(), hits.get( 0 ).getId() );
    }

    @Test
    @DisplayName("search: an id lookup finds the ticket, and an id naming no ticket is simply null")
    public void search_byIdFindsTheTicketOrNothing() {
        Ticket t = openWithTargets( TicketType.CURATION, "search-by-id-" + UUID.randomUUID(), 4 );
        flushAndClear();

        TicketSearchHitValueObject hit = ticketDao.findSearchHitById( t.getId(), true, null );
        assertNotNull( hit );
        assertEquals( 4L, hit.getTargetCount() );
        assertEquals( TicketState.OPEN, hit.getState() );

        assertNull( ticketDao.findSearchHitById( 987_654_321L, true, null ),
                "an id that names no ticket is a non-hit, not an error" );
    }

    @Test
    @DisplayName("search: openOnly excludes a resolved ticket, and dropping it lets the ticket back in")
    public void search_openOnlyExcludesResolvedTickets() {
        String tag = "searchopen-" + UUID.randomUUID();
        Ticket t = openWithTargets( TicketType.CURATION, tag, 1 );
        ticketService.transition( t, TicketState.RESOLVED, reporter, "done" );
        flushAndClear();

        assertTrue( ticketDao.findSearchHitsByTitle( tag, true, null, 20 ).isEmpty() );
        assertEquals( 1, ticketDao.findSearchHitsByTitle( tag, false, null, 20 ).size() );
        assertNull( ticketDao.findSearchHitById( t.getId(), true, null ) );
        assertNotNull( ticketDao.findSearchHitById( t.getId(), false, null ) );
    }

    @Test
    @DisplayName("search: a scratchpad is offered to its own reporter and to nobody else")
    public void search_scratchpadIsScopedToItsReporter() {
        String tag = "searchpad-" + UUID.randomUUID();
        Contact other = new Contact();
        other.setName( "ticket-it-other-" + UUID.randomUUID() );
        other = contactDao.create( other );

        Ticket pad = openWithTargets( TicketType.SCRATCHPAD, tag, 2 );
        flushAndClear();

        assertEquals( 1, ticketDao.findSearchHitsByTitle( tag, true, reporter.getId(), 20 ).size(),
                "a curator's own scratchpad is a reasonable place to file work" );
        assertTrue( ticketDao.findSearchHitsByTitle( tag, true, other.getId(), 20 ).isEmpty(),
                "another curator's scratchpad is not" );
        assertTrue( ticketDao.findSearchHitsByTitle( tag, true, null, 20 ).isEmpty(),
                "and an anonymous caller is offered nobody's" );
        assertNotNull( ticketDao.findSearchHitById( pad.getId(), true, reporter.getId() ) );
        assertNull( ticketDao.findSearchHitById( pad.getId(), true, other.getId() ) );
    }

    @Test
    @DisplayName("search: a wildcard typed into the box is matched literally, not as a wildcard")
    public void search_wildcardsInTheQueryAreEscaped() {
        String tag = UUID.randomUUID().toString().replace( "-", "" );
        Ticket literal = openWithTargets( TicketType.CURATION, tag + " 50% complete", 1 );
        openWithTargets( TicketType.CURATION, tag + " 50 percent complete", 1 );
        flushAndClear();

        // scoped by the tag so the assertion counts only this test's two tickets. Unescaped, the
        // '%' would make this match the "50 percent" title too.
        List<TicketSearchHitValueObject> hits =
                ticketDao.findSearchHitsByTitle( tag + " 50% comp", true, null, 20 );

        assertEquals( 1, hits.size(), "'50% comp' is a literal fragment, not '50' + anything + ' comp'" );
        assertEquals( literal.getId(), hits.get( 0 ).getId() );
    }

    @Test
    @DisplayName("search: title hits come back most-recently-updated first")
    public void search_titleHitsAreOrderedByUpdatedAtDesc() {
        String tag = "searchorder-" + UUID.randomUUID();
        Ticket older = openWithTargets( TicketType.CURATION, tag + " older", 1 );
        Ticket newer = openWithTargets( TicketType.CURATION, tag + " newer", 1 );
        older.setUpdatedAt( new Date( 1_600_000_000_000L ) );
        newer.setUpdatedAt( new Date( 1_700_000_000_000L ) );
        flushAndClear();

        List<TicketSearchHitValueObject> hits = ticketDao.findSearchHitsByTitle( tag, true, null, 20 );

        assertEquals( 2, hits.size() );
        assertEquals( newer.getId(), hits.get( 0 ).getId() );
        assertEquals( older.getId(), hits.get( 1 ).getId() );
    }

    /**
     * A target-id range of this test's own, so a bulk assertion counts only its own fixtures:
     * {@link #openWithTargets} seeds every other test's tickets at 1_000_000+, and those tickets
     * stay visible to a query that asks by target id rather than by title.
     */
    private long freshTargetBase() {
        return 5_000_000L + Math.floorMod( UUID.randomUUID().getMostSignificantBits(), 1_000_000L ) * 10L;
    }

    private Ticket openTargeting( TicketType type, String title, Long... targetIds ) {
        Set<TicketTarget> targets = new HashSet<>();
        for ( Long id : targetIds ) {
            targets.add( TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, id ) );
        }
        return ticketService.openTicket( reporter, type, title, targets );
    }

    @Test
    @DisplayName("bulk: a dataset on no open ticket gets NO key, so an absence means something")
    public void bulkSummaries_quietDatasetIsAbsentNotEmpty() {
        long base = freshTargetBase();
        Long onATicket = base, quiet = base + 1;
        openTargeting( TicketType.CURATION, "bulk-presence-" + UUID.randomUUID(), onATicket );
        flushAndClear();

        Map<Long, List<TicketSearchHitValueObject>> byDataset =
                ticketDao.findOpenSummariesForTargets( TicketTargetType.EXPRESSION_EXPERIMENT,
                        Arrays.asList( onATicket, quiet ) );

        assertEquals( 1, byDataset.size() );
        assertTrue( byDataset.containsKey( onATicket ) );
        assertFalse( byDataset.containsKey( quiet ),
                "an id with no open ticket must be ABSENT — an empty list would cost a page of them" );
    }

    @Test
    @DisplayName("bulk: each dataset gets its OWN tickets, and a resolved one is not among them")
    public void bulkSummaries_groupsByTargetAndExcludesResolved() {
        long base = freshTargetBase();
        Long a = base, b = base + 1;
        Ticket both = openTargeting( TicketType.CURATION, "bulk-both-" + UUID.randomUUID(), a, b );
        Ticket onlyA = openTargeting( TicketType.PRELOAD, "bulk-onlya-" + UUID.randomUUID(), a );
        Ticket resolved = openTargeting( TicketType.GENERIC, "bulk-done-" + UUID.randomUUID(), b );
        ticketService.transition( resolved, TicketState.RESOLVED, reporter, "done" );
        flushAndClear();

        Map<Long, List<TicketSearchHitValueObject>> byDataset =
                ticketDao.findOpenSummariesForTargets( TicketTargetType.EXPRESSION_EXPERIMENT,
                        Arrays.asList( a, b ) );

        Set<Long> forA = new HashSet<>();
        for ( TicketSearchHitValueObject h : byDataset.get( a ) ) {
            forA.add( h.getId() );
        }
        assertEquals( new HashSet<>( Arrays.asList( both.getId(), onlyA.getId() ) ), forA,
                "a dataset on two open tickets gets both, and only its own" );

        assertEquals( Collections.singletonList( both.getId() ),
                byDataset.get( b ).stream().map( TicketSearchHitValueObject::getId )
                        .collect( java.util.stream.Collectors.toList() ),
                "the RESOLVED ticket targeting b is not an open ticket" );
    }

    @Test
    @DisplayName("bulk: the batched answer is the per-dataset answer, ticket for ticket")
    public void bulkSummaries_agreeWithFindOpenForTarget() {
        long base = freshTargetBase();
        Long a = base, b = base + 1, quiet = base + 2;
        openTargeting( TicketType.CURATION, "bulk-agree-1-" + UUID.randomUUID(), a, b );
        openTargeting( TicketType.SCRATCHPAD, "bulk-agree-pad-" + UUID.randomUUID(), a );
        flushAndClear();

        // The glyph on the experiment list and the drawer behind it come from these two routes.
        // A dataset that reads "on a ticket" in one and not the other is the bug this pins.
        Map<Long, List<TicketSearchHitValueObject>> bulk =
                ticketDao.findOpenSummariesForTargets( TicketTargetType.EXPRESSION_EXPERIMENT,
                        Arrays.asList( a, b, quiet ) );
        for ( Long id : Arrays.asList( a, b, quiet ) ) {
            Set<Long> single = new HashSet<>();
            for ( Ticket t : ticketDao.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, id ) ) {
                single.add( t.getId() );
            }
            Set<Long> batched = new HashSet<>();
            for ( TicketSearchHitValueObject h : bulk.getOrDefault( id, Collections.emptyList() ) ) {
                batched.add( h.getId() );
            }
            assertEquals( single, batched, "the two routes disagree about dataset " + id );
        }
    }

    @Test
    @DisplayName("bulk: targetCount is the ticket's whole size, not the slice on this page")
    public void bulkSummaries_targetCountIsTheWholeTicket() {
        long base = freshTargetBase();
        Long a = base, b = base + 1, c = base + 2;
        openTargeting( TicketType.CURATION, "bulk-count-" + UUID.randomUUID(), a, b, c );
        flushAndClear();

        // Asked about ONE of the three members: the count still reports three.
        Map<Long, List<TicketSearchHitValueObject>> byDataset =
                ticketDao.findOpenSummariesForTargets( TicketTargetType.EXPRESSION_EXPERIMENT,
                        Collections.singletonList( a ) );

        assertEquals( 3L, byDataset.get( a ).get( 0 ).getTargetCount() );
    }

    @Test
    @DisplayName("bulk: no ids is an empty map, not an invalid `in ()`")
    public void bulkSummaries_emptyIdsIsAnEmptyMap() {
        assertTrue( ticketDao.findOpenSummariesForTargets( TicketTargetType.EXPRESSION_EXPERIMENT,
                Collections.emptyList() ).isEmpty() );
    }

    /**
     * Regression coverage for the JAX-RS "detached entity / lazy-init" footgun surfaced
     * by the 2026-05-27 frink smoke test:
     *
     * <ol>
     *   <li>{@code GET /tickets/{id}} returned 500 with "Could not initialize proxy
     *       [Contact#1] - no session" — the handler called {@code ticketService.load(id)}
     *       then projected the VO outside the now-closed session; the lazy reporter
     *       Contact failed to materialise.</li>
     *   <li>{@code DELETE /tickets/{id}} returned 500 with "failed to lazily initialize
     *       a collection of role: Ticket.events - could not initialize proxy - no
     *       Session" — the handler loaded the ticket, then passed the detached entity
     *       to {@code transition()}; the new transaction couldn't mutate
     *       {@code ticket.getEvents()} because the collection proxy was bound to the
     *       closed session.</li>
     * </ol>
     *
     * <p>The main-test class is {@code @Transactional}, which keeps a single session open
     * across the whole method and silently hides this bug — that's how the bug shipped to
     * frink unnoticed. This nested class uses {@link TransactionTemplate} to force a
     * commit + close BETWEEN persist and the test action, exactly mirroring the JAX-RS
     * request boundary.</p>
     */
    @Nested
    @DisplayName("Detached-entity / lazy-init regression (JAX-RS boundary simulation)")
    class DetachedEntityRegression {

        @Autowired
        private PlatformTransactionManager txManager;

        private Long persistTicketInOwnTransaction( TicketType type, String title ) {
            TransactionTemplate tx = new TransactionTemplate( txManager );
            return tx.execute( status -> {
                TicketTarget target = TicketTarget.Factory.newInstance(
                        TicketTargetType.EXPRESSION_EXPERIMENT, 1L );
                Ticket created = ticketService.openTicket( reporter, type, title,
                        Collections.singleton( target ) );
                return created.getId();
            } );
        }

        @Test
        @DisplayName("loadValueObject(id, true): VO projection survives after the persist txn closes")
        public void loadValueObject_afterDetach_works() {
            Long id = persistTicketInOwnTransaction( TicketType.QUALITY_REVIEW, "vo-after-detach" );
            // Persist txn has committed; any lazy proxies on a re-loaded Ticket would be
            // bound to a session that no longer exists. The service-side loadValueObject
            // must initialize them inside its own @Transactional.
            TicketValueObject vo = ticketService.loadValueObject( id, true );
            assertNotNull( vo );
            assertNotNull( vo.getReporterId(), "reporter must be initialized" );
            assertNotNull( vo.getReporterName(), "reporter name must be initialized" );
            assertEquals( 1, vo.getTargets().size() );
            assertEquals( 1, vo.getEvents().size(), "single OPENED event" );
            assertEquals( TicketEventType.OPENED, vo.getEvents().get( 0 ).getType() );
            assertNotNull( vo.getEvents().get( 0 ).getActorName(), "event actor must be initialized" );
        }

        @Test
        @DisplayName("transition(detachedTicket): re-attaches inside the txn so events collection works")
        public void transition_onDetachedTicket_works() {
            Long id = persistTicketInOwnTransaction( TicketType.GENERIC, "transition-after-detach" );

            // Reload OUTSIDE a transaction — simulates the JAX-RS handler holding a detached
            // ticket reference between ticketService.load(id) and ticketService.transition(...).
            TransactionTemplate tx = new TransactionTemplate( txManager );
            Ticket detached = tx.execute( status -> ticketDao.load( id ) );
            assertNotNull( detached );

            // The fix: transition() reattaches by id at the top, so this no longer NPEs.
            ticketService.transition( detached, TicketState.CANCELLED, reporter, "smoke-test-cleanup" );

            TicketValueObject vo = ticketService.loadValueObject( id, true );
            assertEquals( TicketState.CANCELLED, vo.getState() );
            assertTrue( vo.getEvents().stream().anyMatch( e -> e.getType() == TicketEventType.CANCELLED ),
                    "CANCELLED event should be on the log" );
        }

        @Test
        @DisplayName("addComment(detachedTicket): re-attaches so comment event lands")
        public void addComment_onDetachedTicket_works() {
            Long id = persistTicketInOwnTransaction( TicketType.GENERIC, "comment-after-detach" );
            TransactionTemplate tx = new TransactionTemplate( txManager );
            Ticket detached = tx.execute( status -> ticketDao.load( id ) );

            ticketService.addComment( detached, reporter, "Looks fine to me" );

            TicketValueObject vo = ticketService.loadValueObject( id, true );
            // The COMMENTED event payload is the JSON-encoded form of "Looks fine to me"
            assertTrue( vo.getEvents().stream().anyMatch( e -> e.getType() == TicketEventType.COMMENTED ),
                    "COMMENTED event should be on the log" );
        }

    }

    /**
     * The LIST paths hand entities back to the web layer, which projects them to VOs AFTER the
     * service transaction closes — the same boundary {@code loadValueObject} was fixed for, but
     * {@code findTickets} / {@code findOpenForTarget} were left behind. On frink that surfaced as a
     * 500 on an unfiltered {@code GET /tickets}: "Could not initialize proxy [Contact#1] - no
     * session". The reporter is a LAZY {@code @ManyToOne}, so the list only survived while every
     * row on the page happened to have no reporter and no assignee.
     *
     * <p>🛑 This class must NOT inherit the outer class's {@code @Transactional} — that is the whole
     * point. A test-managed transaction keeps one session open for the entire method, so entities
     * never actually detach and the projection succeeds whether or not the service initialized
     * anything. {@code @Nested} inherits the enclosing class's annotations, and a
     * {@link TransactionTemplate} inside an ambient transaction merely JOINS it (PROPAGATION_REQUIRED)
     * rather than committing — which is why {@link DetachedEntityRegression} above does not, in fact,
     * reproduce a detached entity. {@code NOT_SUPPORTED} suspends the ambient transaction so the
     * service call opens and CLOSES its own, exactly like a JAX-RS request. Rows committed here are
     * therefore real, so they are torn down explicitly in {@link #cleanup()}.</p>
     */
    @Nested
    @DisplayName("List-path lazy-init regression (genuinely detached: no ambient test transaction)")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class ListPathDetachedRegression {

        @Autowired
        private PlatformTransactionManager txManager;

        private Long ticketId;
        private Long reporterId;
        private Long scratchpadId;

        @BeforeEach
        public void seedCommitted() {
            new TransactionTemplate( txManager ).execute( status -> {
                Contact c = new Contact();
                c.setName( "list-detach-reporter-" + UUID.randomUUID() );
                Contact savedReporter = contactDao.create( c );
                reporterId = savedReporter.getId();
                TicketTarget target = TicketTarget.Factory.newInstance(
                        TicketTargetType.EXPRESSION_EXPERIMENT, 1L );
                Ticket created = ticketService.openTicket( savedReporter, TicketType.CURATION,
                        "list-vo-after-detach-" + UUID.randomUUID(), Collections.singleton( target ) );
                ticketId = created.getId();
                return null;
            } );
        }

        @AfterEach
        public void cleanup() {
            new TransactionTemplate( txManager ).execute( status -> {
                if ( scratchpadId != null ) {
                    Ticket p = ticketDao.load( scratchpadId );
                    if ( p != null ) ticketDao.remove( p );
                }
                if ( ticketId != null ) {
                    Ticket t = ticketDao.load( ticketId );
                    if ( t != null ) ticketDao.remove( t );
                }
                if ( reporterId != null ) {
                    Contact c = contactDao.load( reporterId );
                    if ( c != null ) contactDao.remove( c );
                }
                return null;
            } );
        }


        /**
         * {@code POST /tickets/{id}/targets} 500d on every call, for any target, new or previously
         * removed (cab, 2026-09-01). The handler counted {@code ticket.getTargets()} before and after
         * the add to decide whether to report the id as added — on the ticket it holds DETACHED, whose
         * targets are LAZY. It threw before {@code addTarget} was reached, so the one verb that grows
         * a queue never worked while its mocked test stayed green.
         * <p>
         * 🛑 This must live in the NOT_SUPPORTED class. Under the outer class's {@code @Transactional}
         * the session stays open, the ticket never detaches, and the count succeeds — the assertion
         * below cannot fail there, which is exactly how this shipped.
         */
        @Test
        @DisplayName("addTarget says whether it added, on a ticket the caller holds detached")
        public void addTarget_reportsWhatItDidWithoutReadingTheLazyCollection() {
            Contact curator = new TransactionTemplate( txManager ).execute( status ->
                    contactDao.load( reporterId ) );
            assertNotNull( curator );
            Ticket pad = new TransactionTemplate( txManager ).execute( status ->
                    ticketService.getOrCreateScratchpad( curator ) );
            assertNotNull( pad );
            scratchpadId = pad.getId();

            // exactly what the handler holds: loaded through the service, outside any transaction
            Ticket detached = ticketService.load( scratchpadId );
            assertNotNull( detached );
            assertThrows( LazyInitializationException.class, () -> detached.getTargets().size(),
                    "the targets of a detached ticket cannot be counted by the caller -- "
                            + "the handler that tried is what produced the 500" );

            long targetId = freshTargetBase();
            TicketService.TargetAddition first = ticketService.addTarget( detached,
                    TicketTargetType.EXPRESSION_EXPERIMENT, targetId, curator );
            assertTrue( first.isAdded(), "a target new to the ticket is reported as added" );

            TicketService.TargetAddition again = ticketService.addTarget( first.getTicket(),
                    TicketTargetType.EXPRESSION_EXPERIMENT, targetId, curator );
            assertFalse( again.isAdded(), "re-adding is idempotent and says so rather than erroring" );
        }


        /**
         * {@code GET /tickets/{id}/events} answered 500 "Could not initialize proxy [Contact#1] - no
         * session" in BOTH of its modes (found on gemma2, 2026-09-01). Cursor mode called
         * {@code findEventsByCursor} and mapped to VOs afterwards; legacy mode built the VO from a
         * detached entity. Each event's actor is a LAZY {@code @ManyToOne}, and
         * {@code TicketEventValueObject.from} reads it for every row, so any ticket whose events have
         * an actor -- every ticket, since events record who acted -- failed.
         * <p>
         * The sibling one line above it, {@code findOpenForTargetByCursor}, was already initializing.
         * This one was left behind.
         */
        @Test
        @DisplayName("the event log projects outside the transaction, in both cursor and VO form")
        public void eventProjectionSurvivesDetachment() {
            // cursor mode: the page is mapped to VOs after the service transaction closes
            Ticket loaded = ticketService.load( ticketId );
            assertNotNull( loaded );
            CursorPage<TicketEvent> page = ticketService.findEventsByCursor( loaded, null, 20 );
            assertFalse( page.isEmpty(), "the seeded ticket has at least its OPENED event" );
            List<TicketEventValueObject> vos = page.map( TicketEventValueObject::from );
            for ( TicketEventValueObject vo : vos ) {
                assertNotNull( vo.getActorName(),
                        "each event's actor must be initialized by the service, not read here" );
            }

            // legacy mode reads the same log off the VO the service projects
            TicketValueObject full = ticketService.loadValueObject( ticketId, true );
            assertNotNull( full );
            assertFalse( full.getEvents().isEmpty(), "the event log must survive the projection" );
            assertNotNull( full.getEvents().get( 0 ).getActorName(), "actor must be initialized" );
        }

        /**
         * Project exactly the way {@code TicketsWebService} does — outside any transaction.
         */
        private void assertProjectable( List<Ticket> tickets ) {
            assertFalse( tickets.isEmpty(), "the committed ticket should be listed" );
            boolean sawSeeded = false;
            for ( Ticket t : tickets ) {
                TicketValueObject vo = TicketValueObject.from( t );
                assertNotNull( vo.getTargets(), "targets must be initialized" );
                if ( ticketId.equals( t.getId() ) ) {
                    sawSeeded = true;
                    assertEquals( reporterId, vo.getReporterId(), "reporter must be initialized" );
                    assertNotNull( vo.getReporterName(), "reporter name must be initialized" );
                    assertFalse( vo.getTargets().isEmpty(), "targets must be initialized" );
                }
            }
            assertTrue( sawSeeded, "the seeded ticket should be among the listed tickets" );
        }

        @Test
        @DisplayName("findTickets: VO projection after the service txn closes initializes reporter + targets")
        public void findTickets_projectedAfterDetach_works() {
            assertProjectable( ticketService.findTickets(
                    false, null, null, null, null, null, null, 0, 100 ) );
        }

        @Test
        @DisplayName("findOpenForTarget: VO projection after the service txn closes initializes reporter + targets")
        public void findOpenForTarget_projectedAfterDetach_works() {
            assertProjectable( ticketService.findOpenForTarget(
                    TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) );
        }
    }

    @Test
    @DisplayName("URGENT priority + CANCELLED state round-trip (full TicketPriority + TicketState coverage)")
    public void urgent_cancelled_roundTrip() {
        TicketTarget target = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 1L );
        Ticket created = ticketService.openTicket(
                reporter, TicketType.GENERIC, "urgent-cancel-test",
                Collections.singleton( target ) );
        created.setPriority( TicketPriority.URGENT );
        ticketDao.update( created );
        ticketService.transition( created, TicketState.CANCELLED, reporter, "no longer needed" );
        Long id = created.getId();
        flushAndClear();

        Ticket reloaded = ticketDao.load( id );
        assertEquals( TicketPriority.URGENT, reloaded.getPriority() );
        assertEquals( TicketState.CANCELLED, reloaded.getState() );
    }
}
