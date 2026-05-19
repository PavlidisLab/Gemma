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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Default read-side implementation of {@link CurationDetailsService}. Each
 * method delegates into {@link TicketService#findOpenForTarget} and folds the
 * resulting tickets down to the legacy {@code CurationDetails} field shape.
 *
 * <p>Stateless and read-only; no write methods are implemented yet (see the
 * {@code CurationDetailsService} Javadoc for the migration plan).</p>
 *
 * @author paul
 */
@Service
@SuppressWarnings("deprecation")
public class CurationDetailsServiceImpl implements CurationDetailsService {

    /**
     * Ticket types that historically would have caused
     * {@code curationDetails.needsAttention=true}. Kept as an EnumSet so the
     * lookup is constant-time.
     */
    private static final Set<TicketType> NEEDS_ATTENTION_TYPES = EnumSet.of(
            TicketType.GENERIC,
            TicketType.BATCH_INFO_NEEDED,
            TicketType.QUALITY_REVIEW );

    private static final Set<TicketType> TROUBLED_TYPES = EnumSet.of(
            TicketType.QUALITY_REVIEW );

    private final TicketService ticketService;

    @Autowired
    public CurationDetailsServiceImpl( TicketService ticketService ) {
        this.ticketService = ticketService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean needsAttention( TicketTargetType targetType, Long targetId ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        for ( Ticket t : ticketService.findOpenForTarget( targetType, targetId ) ) {
            if ( NEEDS_ATTENTION_TYPES.contains( t.getType() ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean needsAttention( Curatable curatable ) {
        Assert.notNull( curatable, "Curatable cannot be null." );
        Assert.notNull( curatable.getId(), "Curatable must be persistent (id != null)." );
        return needsAttention( targetTypeFor( curatable ), curatable.getId() );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean troubled( TicketTargetType targetType, Long targetId ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        for ( Ticket t : ticketService.findOpenForTarget( targetType, targetId ) ) {
            if ( TROUBLED_TYPES.contains( t.getType() ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean troubled( Curatable curatable ) {
        Assert.notNull( curatable, "Curatable cannot be null." );
        Assert.notNull( curatable.getId(), "Curatable must be persistent (id != null)." );
        return troubled( targetTypeFor( curatable ), curatable.getId() );
    }

    @Override
    @Nullable
    @Transactional(readOnly = true)
    public Date lastUpdated( TicketTargetType targetType, Long targetId ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        List<Ticket> open = ticketService.findOpenForTarget( targetType, targetId );
        Date best = null;
        for ( Ticket t : open ) {
            for ( TicketEvent e : t.getEvents() ) {
                Date when = e.getOccurredAt();
                if ( when != null && ( best == null || when.after( best ) ) ) {
                    best = when;
                }
            }
        }
        return best;
    }

    @Override
    @Nullable
    @Transactional(readOnly = true)
    public Date lastUpdated( Curatable curatable ) {
        Assert.notNull( curatable, "Curatable cannot be null." );
        Assert.notNull( curatable.getId(), "Curatable must be persistent (id != null)." );
        return lastUpdated( targetTypeFor( curatable ), curatable.getId() );
    }

    /**
     * Maps a {@link Curatable} concrete class to the matching
     * {@link TicketTargetType}. Mirrors the two values currently supported by
     * the ticket layer (Decision 2 of the recce doc).
     */
    private static TicketTargetType targetTypeFor( Curatable curatable ) {
        if ( curatable instanceof ExpressionExperiment ) {
            return TicketTargetType.EXPRESSION_EXPERIMENT;
        }
        if ( curatable instanceof ArrayDesign ) {
            return TicketTargetType.ARRAY_DESIGN;
        }
        throw new IllegalArgumentException( "No TicketTargetType mapping for "
                + curatable.getClass().getName() );
    }
}
