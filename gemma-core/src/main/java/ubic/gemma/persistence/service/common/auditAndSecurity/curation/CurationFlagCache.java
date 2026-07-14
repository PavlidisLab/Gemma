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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the legacy {@code CurationDetails.troubled} / {@code needsAttention} columns in sync with
 * the ticket layer (Tickets Decision D1 — the "ticket-derived cache" variant of task 11). Tickets
 * are the source of truth; this projects their open-set down to the two boolean columns so every
 * existing read / filter / sort against those columns keeps working, cheaply and correctly.
 *
 * <p>Invoked by {@link TicketServiceImpl} whenever a ticket opens or transitions. It never calls
 * back into {@link TicketService} (the caller passes the already-queried open tickets in), so there
 * is no bean cycle. Writes go through the Curatable DAO's {@code update} so the READ_WRITE L2 cache
 * of the EE/AD (which EAGER-joins its {@code CurationDetails}) stays coherent.</p>
 */
@Service
public class CurationFlagCache {

    /**
     * Canonical mapping (single source of truth, shared with {@link CurationDetailsServiceImpl}):
     * an open ticket of one of these types marks the target {@code needsAttention}.
     * {@code PIPELINE_FAILED} is here so task 9's auto-opened pipeline-failure tickets surface.
     */
    public static final Set<TicketType> NEEDS_ATTENTION_TYPES = EnumSet.of(
            TicketType.GENERIC,
            TicketType.BATCH_INFO_NEEDED,
            TicketType.QUALITY_REVIEW,
            TicketType.PIPELINE_FAILED );

    /** An open ticket of one of these types marks the target {@code troubled}. */
    public static final Set<TicketType> TROUBLED_TYPES = EnumSet.of(
            TicketType.QUALITY_REVIEW );

    private final ExpressionExperimentDao expressionExperimentDao;
    private final ArrayDesignDao arrayDesignDao;

    @Autowired
    public CurationFlagCache( ExpressionExperimentDao expressionExperimentDao, ArrayDesignDao arrayDesignDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * Recompute {@code needsAttention}/{@code troubled} for a target from its open tickets and write
     * the columns if they changed. No-op for target types that aren't Curatable (only
     * EXPRESSION_EXPERIMENT / ARRAY_DESIGN). Must run inside the caller's transaction.
     *
     * @param openTickets the target's currently-open tickets (queried by the caller)
     */
    public void apply( TicketTargetType targetType, Long targetId, List<Ticket> openTickets ) {
        boolean needsAttention = false, troubled = false;
        for ( Ticket t : openTickets ) {
            if ( NEEDS_ATTENTION_TYPES.contains( t.getType() ) ) {
                needsAttention = true;
            }
            if ( TROUBLED_TYPES.contains( t.getType() ) ) {
                troubled = true;
            }
        }
        switch ( targetType ) {
            case EXPRESSION_EXPERIMENT: {
                ExpressionExperiment ee = expressionExperimentDao.load( targetId );
                if ( applyToCuratable( ee, needsAttention, troubled ) ) {
                    expressionExperimentDao.update( ee );
                }
                break;
            }
            case ARRAY_DESIGN: {
                ArrayDesign ad = arrayDesignDao.load( targetId );
                if ( applyToCuratable( ad, needsAttention, troubled ) ) {
                    arrayDesignDao.update( ad );
                }
                break;
            }
            default:
                // FACTOR_VALUE / GEO_SCRAPE_WATERMARK / … aren't Curatable — nothing to cache.
        }
    }

    /** @return true if a column actually changed (so the caller should persist). */
    private boolean applyToCuratable( Curatable c, boolean needsAttention, boolean troubled ) {
        if ( c == null || c.getCurationDetails() == null ) {
            return false;
        }
        CurationDetails cd = c.getCurationDetails();
        if ( cd.getNeedsAttention() == needsAttention && cd.getTroubled() == troubled ) {
            return false;
        }
        cd.setNeedsAttention( needsAttention );
        cd.setTroubled( troubled );
        cd.setLastUpdated( new Date() );
        return true;
    }
}
