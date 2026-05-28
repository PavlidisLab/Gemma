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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Hibernate;
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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


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
        Ticket attached = reattach( ticket );
        attached.setAssignee( assignee );
        bumpUpdated( attached );
        appendEvent( attached, TicketEventType.ASSIGNED, actor, null );
        return ticketDao.save( attached );
    }

    @Override
    @Transactional
    public Ticket addComment( Ticket ticket, Contact actor, @Nullable String payload ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        Ticket attached = reattach( ticket );
        bumpUpdated( attached );
        appendEvent( attached, TicketEventType.COMMENTED, actor, payload );
        return ticketDao.save( attached );
    }

    @Override
    @Transactional
    public Ticket transition( Ticket ticket, TicketState newState, Contact actor, @Nullable String reason ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( newState, "Target state cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        ticket = reattach( ticket );
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
    public TicketValueObject loadValueObject( Long id, boolean includeEvents ) {
        Ticket t = ticketDao.load( id );
        if ( t == null ) return null;
        // Force lazy init while the session is still open. Without this, the JAX-RS handler's
        // VO projection runs after the @Transactional ends and raises LazyInitializationException
        // ("no Session") on every lazy field — reporter (LAZY @ManyToOne), assignee, targets,
        // events, plus each event's actor.
        if ( t.getReporter() != null ) Hibernate.initialize( t.getReporter() );
        if ( t.getAssignee() != null ) Hibernate.initialize( t.getAssignee() );
        Hibernate.initialize( t.getTargets() );
        if ( includeEvents ) {
            Hibernate.initialize( t.getEvents() );
            for ( TicketEvent e : t.getEvents() ) {
                if ( e.getActor() != null ) Hibernate.initialize( e.getActor() );
            }
        }
        return TicketValueObject.from( t, includeEvents );
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
    public List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince, int offset, int limit ) {
        return ticketDao.findTickets( openOnly, assigneeId, priority, type, state, targetType, updatedSince, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority ) {
        return ticketDao.countTickets( openOnly, assigneeId, priority );
    }

    @Override
    @Transactional(readOnly = true)
    public long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince ) {
        return ticketDao.countTickets( openOnly, assigneeId, priority, type, state, targetType, updatedSince );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable Cursor cursor, int limit ) {
        return ticketDao.findTicketsByCursor( openOnly, assigneeId, priority, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince,
            @Nullable Cursor cursor, int limit ) {
        return ticketDao.findTicketsByCursor( openOnly, assigneeId, priority, type, state, targetType, updatedSince, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findOpenForTargetByCursor( TicketTargetType targetType, Long targetId,
            @Nullable Cursor cursor, int limit ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        return ticketDao.findOpenForTargetByCursor( targetType, targetId, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<TicketEvent> findEventsByCursor( Ticket ticket, @Nullable Cursor cursor, int limit ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        return ticketDao.findEventsByCursor( ticket, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TicketType, Long> countOpenByType() {
        return ticketDao.countOpenByType();
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpen() {
        return ticketDao.countOpen();
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public Date findOldestOpenCreatedAt() {
        return ticketDao.findOldestOpenCreatedAt();
    }

    private static void bumpUpdated( Ticket t ) {
        t.setUpdatedAt( new Date() );
    }

    /**
     * Reload {@code ticket} by id so that lazy collections (notably {@code getEvents()})
     * resolve through the CURRENT transaction's session. A caller that holds a Ticket
     * loaded in an earlier transaction has a detached entity — touching its lazy
     * collections raises {@code LazyInitializationException} ("no Session"). Every
     * write-side service method (assign / addComment / transition) goes through here
     * to keep that bug class out of the REST and CLI surfaces.
     *
     * <p>A {@code null} id means the entity is transient (e.g. inside a unit test, or
     * inside {@link #openTicket} before the create), and is by definition already
     * attached to this session — return it unchanged.</p>
     */
    private Ticket reattach( Ticket ticket ) {
        if ( ticket.getId() == null ) {
            return ticket;
        }
        Ticket attached = ticketDao.load( ticket.getId() );
        if ( attached == null ) {
            throw new IllegalStateException( "Ticket " + ticket.getId() + " disappeared between load and mutation." );
        }
        return attached;
    }

    /** Module-private singleton; Jackson's ObjectMapper is thread-safe after configuration. */
    private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper();

    /**
     * Append a {@link TicketEvent}. The {@code payload} parameter is a free-form curator-supplied
     * string (reason, comment, etc.); it gets JSON-string-encoded here so the MySQL {@code JSON}
     * column ({@code TICKET_EVENT.PAYLOAD}) accepts it. Without this wrapping, a raw string like
     * {@code "starting work"} fails MySQL's JSON validator at insert time. Surfaces a known
     * limitation: a caller passing already-JSON-shaped text gets it double-encoded — for now no
     * callers do that; if the need arises, add a {@code String json} variant that skips wrapping.
     */
    private static void appendEvent( Ticket t, TicketEventType type, Contact actor, @Nullable String payload ) {
        String jsonPayload;
        if ( payload == null ) {
            jsonPayload = null;
        } else {
            try {
                jsonPayload = PAYLOAD_MAPPER.writeValueAsString( payload );
            } catch ( JsonProcessingException e ) {
                throw new IllegalStateException( "Failed to JSON-encode ticket event payload", e );
            }
        }
        TicketEvent e = TicketEvent.Factory.newInstance( type, actor, jsonPayload );
        e.setTicket( t );
        t.getEvents().add( e );
    }
}
