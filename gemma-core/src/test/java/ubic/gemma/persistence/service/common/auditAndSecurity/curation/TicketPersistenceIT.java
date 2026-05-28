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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketMode;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
