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
package ubic.gemma.model.common.auditAndSecurity.curation;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure VO-level tests for the body / mode / status / display fields added in the
 * curation-UI alignment pass. Verifies the {@code from} factories propagate the
 * new entity fields, and that defaults match the wire contract documented in
 * {@code gemma-curation-ui/apps/curation/src/api/tickets.ts}.
 *
 * <p>Held alongside the entity classes (no Spring context) so a regression in
 * the VO shape surfaces in surefire, not just failsafe.</p>
 */
class TicketValueObjectTest {

    private static Ticket newTicket() {
        Contact reporter = new Contact();
        reporter.setId( 1L );
        reporter.setName( "alice" );
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "Curate GSE12345", reporter );
        t.setId( 100L );
        t.setState( TicketState.OPEN );
        t.setPriority( TicketPriority.NORMAL );
        t.setCreatedAt( new Date() );
        t.setUpdatedAt( new Date() );
        return t;
    }

    @Test
    void from_defaults_body_to_empty_and_mode_to_MANUAL() {
        Ticket t = newTicket();
        // body and mode left untouched on the entity — entity defaults are description=null, mode=MANUAL.
        TicketValueObject vo = TicketValueObject.from( t );

        assertThat( vo.getBody() ).isEqualTo( "" ); // null body → "" on the wire
        assertThat( vo.getMode() ).isEqualTo( TicketMode.MANUAL );
    }

    @Test
    void from_propagates_body_via_description_alias() {
        Ticket t = newTicket();
        t.setBody( "Please curate the time-course factor values; baseline is Day 0." );

        TicketValueObject vo = TicketValueObject.from( t );

        assertThat( vo.getBody() )
                .isEqualTo( "Please curate the time-course factor values; baseline is Day 0." );
        // Body is the inherited DESCRIPTION column under the hood.
        assertThat( t.getDescription() ).isEqualTo( vo.getBody() );
    }

    @Test
    void from_propagates_mode_AUTO() {
        Ticket t = newTicket();
        t.setMode( TicketMode.AUTO );

        TicketValueObject vo = TicketValueObject.from( t );

        assertThat( vo.getMode() ).isEqualTo( TicketMode.AUTO );
    }

    @Test
    void from_propagates_all_new_ticket_types() {
        // The agent + UI rely on PRELOAD and CURATION enum values landing on the wire as-is.
        for ( TicketType type : new TicketType[] {
                TicketType.PRELOAD, TicketType.CURATION, TicketType.GENERIC,
                TicketType.BATCH_INFO_NEEDED, TicketType.REALIGNMENT_NEEDED, TicketType.QUALITY_REVIEW
        } ) {
            Contact reporter = new Contact();
            reporter.setId( 1L );
            Ticket t = Ticket.Factory.newInstance( type, "Ticket " + type, reporter );
            t.setId( 1L );
            t.setCreatedAt( new Date() );
            t.setUpdatedAt( new Date() );

            TicketValueObject vo = TicketValueObject.from( t );

            assertThat( vo.getType() ).as( "type round-trip for %s", type ).isEqualTo( type );
        }
    }

    @Test
    void targetVO_from_defaults_status_to_NOT_DONE() {
        TicketTarget tt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 42L );
        // Status not set explicitly — entity default kicks in.
        TicketTargetValueObject vo = TicketTargetValueObject.from( tt );

        assertThat( vo.getStatus() ).isEqualTo( TicketTargetStatus.NOT_DONE );
        assertThat( vo.getTargetType() ).isEqualTo( TicketTargetType.EXPRESSION_EXPERIMENT );
        assertThat( vo.getTargetId() ).isEqualTo( 42L );
        assertThat( vo.getDisplayLabel() ).isNull();
        assertThat( vo.getDisplayName() ).isNull();
    }

    @Test
    void targetVO_from_propagates_UNDERWAY_and_DONE() {
        for ( TicketTargetStatus s : new TicketTargetStatus[] {
                TicketTargetStatus.UNDERWAY, TicketTargetStatus.DONE
        } ) {
            TicketTarget tt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 42L );
            tt.setStatus( s );

            TicketTargetValueObject vo = TicketTargetValueObject.from( tt );

            assertThat( vo.getStatus() ).as( "status round-trip for %s", s ).isEqualTo( s );
        }
    }

    @Test
    void targetVO_supports_GEO_SCRAPE_WATERMARK() {
        // The agent files per-batch tickets against the GeoScrapeWatermark row; verify
        // the enum value round-trips so the dashboard can render those tickets.
        TicketTarget tt = TicketTarget.Factory.newInstance(
                TicketTargetType.GEO_SCRAPE_WATERMARK, 7L );
        tt.setStatus( TicketTargetStatus.UNDERWAY );

        TicketTargetValueObject vo = TicketTargetValueObject.from( tt );

        assertThat( vo.getTargetType() ).isEqualTo( TicketTargetType.GEO_SCRAPE_WATERMARK );
        assertThat( vo.getStatus() ).isEqualTo( TicketTargetStatus.UNDERWAY );
    }

    @Test
    void from_includeEvents_false_leaves_event_list_empty() {
        Ticket t = newTicket();
        t.setMode( TicketMode.AUTO );
        // Note: t.getEvents() is empty in this lightweight fixture; the assertion here is that
        // the list-view from() factory doesn't accidentally bypass the includeEvents flag now
        // that the from method propagates extra fields.
        TicketValueObject vo = TicketValueObject.from( t );

        assertThat( vo.getEvents() ).isEmpty();
        assertThat( vo.getMode() ).isEqualTo( TicketMode.AUTO ); // sanity: mode still set
    }

    @Test
    void entity_default_mode_is_MANUAL_when_constructed_via_Factory() {
        Contact reporter = new Contact();
        reporter.setId( 1L );
        Ticket t = Ticket.Factory.newInstance( TicketType.GENERIC, "Default mode test", reporter );

        assertThat( t.getMode() ).isEqualTo( TicketMode.MANUAL );
    }

    @Test
    void targetEntity_default_status_is_NOT_DONE_when_constructed_via_Factory() {
        TicketTarget tt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 1L );

        assertThat( tt.getStatus() ).isEqualTo( TicketTargetStatus.NOT_DONE );
    }
}
