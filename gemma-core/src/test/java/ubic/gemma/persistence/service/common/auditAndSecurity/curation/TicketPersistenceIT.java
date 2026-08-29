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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketMode;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
