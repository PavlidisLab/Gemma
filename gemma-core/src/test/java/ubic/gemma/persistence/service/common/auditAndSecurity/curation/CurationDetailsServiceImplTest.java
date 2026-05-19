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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lightweight Mockito unit tests for {@link CurationDetailsServiceImpl}, the
 * compatibility shim that exposes the legacy {@code CurationDetails} read API
 * over the Phase B-1 ticket layer (see {@code AUDIT_AS_WORKFLOW_RECCE.md}
 * Decision 1). Verifies the three field mappings:
 *
 * <ul>
 *   <li>{@code needsAttention} &harr; any open ticket of type
 *       {@code GENERIC} / {@code BATCH_INFO_NEEDED} / {@code QUALITY_REVIEW}</li>
 *   <li>{@code troubled} &harr; any open ticket of type
 *       {@code QUALITY_REVIEW}</li>
 *   <li>{@code lastUpdated} &harr; {@code max(TicketEvent.occurredAt)} across
 *       open tickets, or {@code null} when none exist</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("deprecation")
public class CurationDetailsServiceImplTest {

    private static final Long EE_ID = 100L;
    private static final Long AD_ID = 200L;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private CurationDetailsServiceImpl service;

    private Contact reporter;

    @Before
    public void setUp() {
        reporter = new Contact();
        reporter.setId( 11L );
        reporter.setName( "Reporter" );
    }

    // ---------- needsAttention ---------------------------------------------

    @Test
    public void needsAttention_noOpenTickets_isFalse() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.emptyList() );
        assertFalse( service.needsAttention( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void needsAttention_openGenericTicket_isTrue() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.GENERIC ) ) );
        assertTrue( service.needsAttention( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void needsAttention_openBatchInfoTicket_isTrue() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.BATCH_INFO_NEEDED ) ) );
        assertTrue( service.needsAttention( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void needsAttention_openQualityReviewTicket_isTrue() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.QUALITY_REVIEW ) ) );
        assertTrue( service.needsAttention( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void needsAttention_openRealignmentTicket_isFalse() {
        // REALIGNMENT_NEEDED is not in the legacy needs-attention bucket
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.REALIGNMENT_NEEDED ) ) );
        assertFalse( service.needsAttention( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    // ---------- troubled ---------------------------------------------------

    @Test
    public void troubled_noOpenTickets_isFalse() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.emptyList() );
        assertFalse( service.troubled( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void troubled_qualityReviewOpen_isTrue() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.QUALITY_REVIEW ) ) );
        assertTrue( service.troubled( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void troubled_genericOnly_isFalse() {
        // A GENERIC ticket means "needs attention" but NOT "troubled" under
        // the recce-doc mapping.
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.GENERIC ) ) );
        assertFalse( service.troubled( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    // ---------- lastUpdated ------------------------------------------------

    @Test
    public void lastUpdated_noOpenTickets_isNull() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.emptyList() );
        assertNull( service.lastUpdated( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    @Test
    public void lastUpdated_returnsMaxOccurredAtAcrossOpenTickets() {
        Date t0 = new Date( 1_000_000_000_000L );
        Date t1 = new Date( 1_000_000_010_000L );
        Date t2 = new Date( 1_000_000_020_000L );

        Ticket older = ticketOfType( TicketType.GENERIC );
        older.getEvents().add( eventAt( t0 ) );
        older.getEvents().add( eventAt( t1 ) );

        Ticket newer = ticketOfType( TicketType.QUALITY_REVIEW );
        newer.getEvents().add( eventAt( t2 ) );

        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Arrays.asList( older, newer ) );

        assertEquals( t2, service.lastUpdated( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) );
    }

    // ---------- Curatable convenience overloads ---------------------------

    @Test
    public void curatableOverload_routesExpressionExperimentToEEType() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( EE_ID );

        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.GENERIC ) ) );

        assertTrue( service.needsAttention( ee ) );
        verify( ticketService ).findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, EE_ID );
    }

    @Test
    public void curatableOverload_routesArrayDesignToADType() {
        ArrayDesign ad = new ArrayDesign();
        ad.setId( AD_ID );

        when( ticketService.findOpenForTarget( TicketTargetType.ARRAY_DESIGN, AD_ID ) )
                .thenReturn( Collections.singletonList( ticketOfType( TicketType.QUALITY_REVIEW ) ) );

        assertTrue( service.troubled( ad ) );
        verify( ticketService ).findOpenForTarget( TicketTargetType.ARRAY_DESIGN, AD_ID );
    }

    @Test
    public void curatableOverload_rejectsTransientCuratable() {
        ExpressionExperiment ee = new ExpressionExperiment(); // no id set
        assertThrows( IllegalArgumentException.class, () -> service.needsAttention( ee ) );
    }

    // ---------- argument validation ---------------------------------------

    @Test
    public void rejectsNullTargetType() {
        assertThrows( IllegalArgumentException.class,
                () -> service.needsAttention( null, EE_ID ) );
    }

    @Test
    public void rejectsNullTargetId() {
        assertThrows( IllegalArgumentException.class,
                () -> service.troubled( TicketTargetType.EXPRESSION_EXPERIMENT, null ) );
    }

    // ---- helpers ----------------------------------------------------------

    private Ticket ticketOfType( TicketType type ) {
        Ticket t = new Ticket();
        t.setType( type );
        t.setReporter( reporter );
        t.setTitle( "t" );
        return t;
    }

    private TicketEvent eventAt( Date when ) {
        TicketEvent e = new TicketEvent();
        e.setType( TicketEventType.STATE_CHANGED );
        e.setActor( reporter );
        e.setOccurredAt( when );
        return e;
    }

    @SuppressWarnings("unused")
    private static List<Ticket> tickets( Ticket... ts ) {
        return Arrays.asList( ts );
    }
}
