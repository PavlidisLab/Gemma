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
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Phase B-1 implementation of {@link TicketService}. Each mutating method
 * appends a {@link TicketEvent} to the ticket; persistence is via the HBM
 * cascade on the {@code events} set (no external event bus, per the recce
 * doc's "first slice" constraint).
 *
 * <p>{@code @Audited} integration and the REST surface are explicitly
 * deferred to Phase B-2.</p>
 *
 * @author paul
 */
@Service
public class TicketServiceImpl extends AbstractService<Ticket> implements TicketService {

    private final TicketDao ticketDao;

    @Autowired
    public TicketServiceImpl( TicketDao ticketDao ) {
        super( ticketDao );
        this.ticketDao = ticketDao;
    }

    @Override
    @Transactional
    public Ticket openTicket( Contact reporter, TicketType type, String title, Collection<TicketTarget> targets ) {
        Assert.notNull( reporter, "Reporter cannot be null." );
        Assert.notNull( type, "TicketType cannot be null." );
        Assert.hasText( title, "Title must be non-blank." );
        Assert.notEmpty( targets, "A ticket needs at least one target." );

        Ticket t = Ticket.Factory.newInstance( type, title, reporter );
        Date now = new Date();
        t.setCreatedAt( now );
        t.setUpdatedAt( now );
        for ( TicketTarget tgt : targets ) {
            tgt.setTicket( t );
            t.getTargets().add( tgt );
        }
        appendEvent( t, TicketEventType.OPENED, reporter, null );
        return ticketDao.create( t );
    }

    @Override
    @Transactional
    public Ticket assign( Ticket ticket, Contact actor, @Nullable Contact assignee ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        ticket.setAssignee( assignee );
        bumpUpdated( ticket );
        appendEvent( ticket, TicketEventType.ASSIGNED, actor, null );
        return ticketDao.save( ticket );
    }

    @Override
    @Transactional
    public Ticket addComment( Ticket ticket, Contact actor, @Nullable String payload ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        bumpUpdated( ticket );
        appendEvent( ticket, TicketEventType.COMMENTED, actor, payload );
        return ticketDao.save( ticket );
    }

    @Override
    @Transactional
    public Ticket transition( Ticket ticket, TicketState newState, Contact actor, @Nullable String reason ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( newState, "Target state cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        TicketState old = ticket.getState();
        if ( old == newState ) {
            // no-op transition; don't pollute the event log
            return ticket;
        }
        ticket.setState( newState );
        bumpUpdated( ticket );

        TicketEventType eventType;
        switch ( newState ) {
            case RESOLVED:
                eventType = TicketEventType.RESOLVED;
                break;
            case CANCELLED:
                eventType = TicketEventType.CANCELLED;
                break;
            case OPEN:
                eventType = ( old == TicketState.RESOLVED || old == TicketState.CANCELLED )
                        ? TicketEventType.REOPENED
                        : TicketEventType.STATE_CHANGED;
                break;
            default:
                eventType = TicketEventType.STATE_CHANGED;
        }
        appendEvent( ticket, eventType, actor, reason );
        return ticketDao.save( ticket );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        return ticketDao.findOpenForTarget( targetType, targetId );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findAssignedTo( Contact assignee ) {
        Assert.notNull( assignee, "Assignee cannot be null." );
        return ticketDao.findAssignedTo( assignee );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority, int offset, int limit ) {
        return ticketDao.findTickets( openOnly, assigneeId, priority, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority ) {
        return ticketDao.countTickets( openOnly, assigneeId, priority );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable Cursor cursor, int limit ) {
        return ticketDao.findTicketsByCursor( openOnly, assigneeId, priority, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findOpenForTargetByCursor( TicketTargetType targetType, Long targetId,
            @Nullable Cursor cursor, int limit ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        return ticketDao.findOpenForTargetByCursor( targetType, targetId, cursor, limit );
    }

    private static void bumpUpdated( Ticket t ) {
        t.setUpdatedAt( new Date() );
    }

    private static void appendEvent( Ticket t, TicketEventType type, Contact actor, @Nullable String payload ) {
        TicketEvent e = TicketEvent.Factory.newInstance( type, actor, payload );
        e.setTicket( t );
        t.getEvents().add( e );
    }
}
