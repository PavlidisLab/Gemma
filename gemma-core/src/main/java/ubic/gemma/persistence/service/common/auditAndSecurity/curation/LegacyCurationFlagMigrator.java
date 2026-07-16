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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One-time forward-migration of the legacy {@code CurationDetails.troubled}/{@code needsAttention}
 * flags into Tickets (task 11). Before the ticket layer, those flags were curation signal set by
 * {@code TroubledStatusFlagEvent}/{@code NeedsAttentionEvent}; the write side has since moved to
 * tickets, leaving the columns frozen. This turns each still-set legacy flag into an open ticket so
 * tickets become the source of truth WITHOUT losing the signal — it never clears a flag.
 *
 * <p>Scoped to {@link ExpressionExperiment} (the curator-facing "datasets"); ArrayDesign flag
 * migration is a deferred follow-up (AD columns keep working untouched, and the EE trouble read-fold
 * still consults them). Idempotent: re-running skips datasets already covered by an open ticket.</p>
 *
 * <p>Run once at deploy via the {@code migrateCurationFlagsToTickets} CLI. The live
 * {@link CurationFlagCache} hook keeps everything correct afterwards.</p>
 */
@Service
@Slf4j
public class LegacyCurationFlagMigrator {

    private static final TicketTargetType EE = TicketTargetType.EXPRESSION_EXPERIMENT;

    private final ExpressionExperimentDao expressionExperimentDao;
    private final TicketService ticketService;
    private final CurationFlagCache curationFlagCache;

    @Autowired
    public LegacyCurationFlagMigrator( ExpressionExperimentDao expressionExperimentDao,
            TicketService ticketService, CurationFlagCache curationFlagCache ) {
        this.expressionExperimentDao = expressionExperimentDao;
        this.ticketService = ticketService;
        this.curationFlagCache = curationFlagCache;
    }

    /**
     * @param operator the Contact recorded as the migration tickets' reporter
     * @return number of migration tickets opened (datasets already covered by an open ticket are skipped)
     */
    @Transactional
    public int migrate( Contact operator ) {
        // Pass 1: turn still-set legacy flags into tickets (the forward-migration; never clears).
        Set<Long> flagged = new LinkedHashSet<>();
        flagged.addAll( expressionExperimentDao.loadTroubledIds() );
        flagged.addAll( expressionExperimentDao.loadNeedsAttentionIds() );

        int opened = 0;
        for ( Long id : flagged ) {
            ExpressionExperiment ee = expressionExperimentDao.load( id );
            if ( ee == null || ee.getCurationDetails() == null ) {
                continue;
            }
            if ( openMigrationTicket( operator, ee ) ) {
                opened++;
            }
        }

        // Pass 2: reconcile the column for every EE already targeted by an open ticket — catches
        // tickets opened during the frozen window (before the live hook existed) whose column is
        // still stale-false. Uses the same projection as the live cache; never clears a real flag.
        int synced = 0;
        Set<Long> reconciled = new LinkedHashSet<>();
        for ( Ticket t : ticketService.findTickets( true, null, null, 0, Integer.MAX_VALUE ) ) {
            for ( TicketTarget tgt : t.getTargets() ) {
                if ( tgt.getTargetType() == EE && reconciled.add( tgt.getTargetId() ) ) {
                    curationFlagCache.apply( EE, tgt.getTargetId(),
                            ticketService.findOpenForTarget( EE, tgt.getTargetId() ) );
                    synced++;
                }
            }
        }
        log.info( "Legacy curation-flag migration: opened {} tickets, reconciled {} ticketed datasets.",
                opened, synced );
        return opened;
    }

    /**
     * Open a migration ticket for a legacy-flagged EE if one doesn't already cover it. troubled →
     * QUALITY_REVIEW (which also implies needs-attention); needs-attention-only → GENERIC. Dates the
     * ticket from the flag's audit-event pointer, falling back safely when absent.
     *
     * @return true if a ticket was opened, false if already covered (idempotent no-op)
     */
    private boolean openMigrationTicket( Contact operator, ExpressionExperiment ee ) {
        CurationDetails cd = ee.getCurationDetails();
        boolean troubled = cd.getTroubled();
        boolean needsAttention = cd.getNeedsAttention();
        if ( !troubled && !needsAttention ) {
            return false;
        }
        List<Ticket> open = ticketService.findOpenForTarget( EE, ee.getId() );
        boolean hasTroubledTicket = open.stream().anyMatch( t -> CurationFlagCache.TROUBLED_TYPES.contains( t.getType() ) );
        boolean hasNeedsAttentionTicket = open.stream().anyMatch( t -> CurationFlagCache.NEEDS_ATTENTION_TYPES.contains( t.getType() ) );

        TicketType type;
        if ( troubled && !hasTroubledTicket ) {
            type = TicketType.QUALITY_REVIEW;  // implies both troubled + needsAttention
        } else if ( needsAttention && !hasNeedsAttentionTicket ) {
            type = TicketType.GENERIC;         // implies needsAttention only
        } else {
            return false;                      // an open ticket already covers the set flag(s)
        }

        Date troubledDate = dateOf( cd.getLastTroubledEvent() );
        Date needsAttentionDate = dateOf( cd.getLastNeedsAttentionEvent() );
        Date createdAt = firstNonNull( latest( troubledDate, needsAttentionDate ), cd.getLastUpdated(), new Date() );
        String flag = ( troubled && needsAttention ) ? "troubled+needsAttention" : ( troubled ? "troubled" : "needsAttention" );
        String label = ee.getShortName() != null ? ee.getShortName() : ( "EE " + ee.getId() );

        Ticket ticket = ticketService.openTicket( operator, type,
                "Migrated from legacy CurationDetails (" + flag + ") — " + label,
                Collections.singleton( TicketTarget.Factory.newInstance( EE, ee.getId() ) ) );
        ticket.setCreatedAt( createdAt );
        ticketService.update( ticket );
        ticketService.addComment( ticket, operator, "{\"source\":\"legacy-curation-details\",\"flag\":\"" + flag + "\""
                + ",\"troubledDate\":" + jsonDate( troubledDate )
                + ",\"needsAttentionDate\":" + jsonDate( needsAttentionDate ) + "}" );
        return true;
    }

    private static Date dateOf( AuditEvent e ) {
        return e != null ? e.getDate() : null;
    }

    private static Date latest( Date a, Date b ) {
        if ( a == null ) return b;
        if ( b == null ) return a;
        return a.after( b ) ? a : b;
    }

    @SafeVarargs
    private static <T> T firstNonNull( T... vals ) {
        for ( T v : vals ) {
            if ( v != null ) return v;
        }
        return null;
    }

    private static String jsonDate( Date d ) {
        return d != null ? String.valueOf( d.getTime() ) : "null";
    }
}
