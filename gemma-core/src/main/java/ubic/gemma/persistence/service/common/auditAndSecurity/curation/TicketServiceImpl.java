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
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
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
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Implementation of {@link TicketService}. Each mutating method writes to
 * BOTH log streams (Decision 6 of {@code AUDIT_AS_WORKFLOW_RECCE.md}):
 * <ul>
 *   <li>The domain-workflow {@link TicketEvent} stream — append-only, with
 *       the ticket-shaped {@link TicketEventType} enum carrying the action.</li>
 *   <li>The governance {@code AuditTrail} stream inherited from
 *       {@link ubic.gemma.model.common.auditAndSecurity.AbstractAuditable},
 *       populated via {@code @Audited}-annotated event types
 *       ({@link TicketOpenedEvent}, {@link TicketAssignedEvent},
 *       {@link CommentedEvent}, {@link TicketStateChangedEvent}).</li>
 * </ul>
 * For {@code openTicket} the {@code @Audited} aspect can't target the result
 * (it inspects method args), so the {@link TicketOpenedEvent} row is written
 * inline via {@link AuditTrailService} after the ticket is created.
 *
 * @author paul
 */
@Service
public class TicketServiceImpl extends AbstractService<Ticket> implements TicketService {

    private final TicketDao ticketDao;
    private final AuditTrailService auditTrailService;

    @Autowired
    public TicketServiceImpl( TicketDao ticketDao, AuditTrailService auditTrailService ) {
        super( ticketDao );
        this.ticketDao = ticketDao;
        this.auditTrailService = auditTrailService;
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
        Ticket created = ticketDao.create( t );
        // @Audited targets the first Auditable method argument; openTicket
        // has none (the Ticket is constructed inside), so write the
        // companion AuditTrail row inline after persistence.
        auditTrailService.addUpdateEvent( created, TicketOpenedEvent.class,
                "Opened ticket '" + title + "' (type=" + type + ")" );
        return created;
    }

    @Override
    @Transactional
    @Audited(value = TicketAssignedEvent.class,
            messageSpel = "'Assignee ' + (#assignee != null ? '-> ' + #assignee.getId() : 'cleared')")
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
    @Audited(value = CommentedEvent.class, message = "Ticket comment added")
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
        Ticket attached = reattach( ticket );
        TicketState old = attached.getState();
        if ( old == newState ) {
            // no-op transition; don't pollute either log stream.
            return attached;
        }
        attached.setState( newState );
        bumpUpdated( attached );

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
        appendEvent( attached, eventType, actor, reason );
        Ticket saved = ticketDao.save( attached );
        // @AuditedConditional can't cleanly express "fired only on real
        // transitions" because the predicate runs AFTER the method mutates
        // the entity, and in the common test/REST flow the arg and the
        // attached/result instances share session identity (so
        // #result.state == #ticket.state at evaluation time). Inline emit
        // covers exactly the real-transition branch.
        auditTrailService.addUpdateEvent( saved, TicketStateChangedEvent.class,
                old + " -> " + newState + ( reason != null && !reason.isEmpty() ? ": " + reason : "" ) );
        return saved;
    }

    @Override
    @Transactional
    @Audited(value = TicketMetadataChangedEvent.class, messageSpel = "'changed: ' + #changedFields")
    public Ticket updateMetadata( Ticket ticket, String changedFields ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.hasText( changedFields, "changedFields must be non-blank — caller computes the diff." );
        Ticket attached = reattach( ticket );
        // The caller already mutated the metadata fields on the ticket arg.
        // If `attached` is a different instance from `ticket` (session miss),
        // copy the mutated fields across. In the common REST flow ticket was
        // loaded by ticketService.load(id) earlier in the same transaction, so
        // attached == ticket and this is a no-op.
        if ( attached != ticket ) {
            attached.setPriority( ticket.getPriority() );
            attached.setDueDate( ticket.getDueDate() );
            attached.setTitle( ticket.getTitle() );
            attached.setBody( ticket.getBody() );
            attached.setMode( ticket.getMode() );
        }
        bumpUpdated( attached );
        return ticketDao.save( attached );
    }

    @Override
    @Transactional
    public Ticket updateTargetStatus( Ticket ticket, Long targetId, TicketTargetStatus newStatus, Contact actor ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( targetId, "targetId cannot be null." );
        Assert.notNull( newStatus, "newStatus cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        Ticket attached = reattach( ticket );
        TicketTarget tgt = null;
        for ( TicketTarget t : attached.getTargets() ) {
            if ( targetId.equals( t.getId() ) ) {
                tgt = t;
                break;
            }
        }
        if ( tgt == null ) {
            throw new IllegalArgumentException( "Ticket " + attached.getId()
                    + " has no target with id " + targetId );
        }
        TicketTargetStatus old = tgt.getStatus();
        if ( old == newStatus ) {
            // no-op; don't pollute either log stream.
            return attached;
        }
        tgt.setStatus( newStatus );
        bumpUpdated( attached );

        String summary = "target " + tgt.getTargetId()
                + " (" + tgt.getTargetType() + "): "
                + old + " -> " + newStatus;
        appendEvent( attached, TicketEventType.TARGET_STATUS_CHANGED, actor, summary );
        Ticket saved = ticketDao.save( attached );
        // Same session-identity rationale as transition() — emit the
        // companion AuditTrail row inline rather than via @AuditedConditional.
        auditTrailService.addUpdateEvent( saved, TicketTargetStatusChangedEvent.class, summary );
        return saved;
    }

    @Override
    @Transactional
    public Ticket updateTargetScreeningResult( Ticket ticket, Long targetId,
            @Nullable ScreeningResult newResult, @Nullable String newReason, boolean reasonProvided, Contact actor ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( targetId, "targetId cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        Ticket attached = reattach( ticket );
        TicketTarget tgt = null;
        for ( TicketTarget t : attached.getTargets() ) {
            if ( targetId.equals( t.getId() ) ) {
                tgt = t;
                break;
            }
        }
        if ( tgt == null ) {
            throw new IllegalArgumentException( "Ticket " + attached.getId()
                    + " has no target with id " + targetId );
        }
        ScreeningResult old = tgt.getScreeningResult();
        String oldReason = tgt.getScreeningResultReason();
        // The reason is independent of the decision: an absent reason key leaves it as-is, so
        // re-sending the same decision without a reason does NOT wipe the note (that silent loss
        // was the pre-fix behaviour). Only an explicit reason changes or clears it. Clearing the
        // reason when the DECISION changes is the client's call — send reason=null then.
        String effectiveReason = reasonProvided ? newReason : oldReason;
        boolean resultChanged = old != newResult;
        boolean reasonChanged = reasonProvided && !java.util.Objects.equals( oldReason, newReason );
        if ( !resultChanged && !reasonChanged ) {
            // no-op; don't pollute the log stream.
            return attached;
        }
        tgt.setScreeningResult( newResult );
        tgt.setScreeningResultReason( effectiveReason );
        bumpUpdated( attached );

        String summary = "target " + tgt.getTargetId()
                + " (" + tgt.getTargetType() + "): screeningResult "
                + old + " -> " + newResult
                + ( reasonChanged ? " (reason updated)" : "" );
        appendEvent( attached, TicketEventType.SCREENING_RESULT_CHANGED, actor, summary );
        // No AuditTrail companion: the screening result is uncoupled working state and its
        // record is the ticket event log. Adding a bespoke AuditEvent subclass here would also
        // trip the Gemma 1.0 audit-type-compatibility surface for no benefit.
        return ticketDao.save( attached );
    }

    @Override
    @Transactional(readOnly = true)
    public TicketValueObject loadValueObject( Long id, boolean includeEvents ) {
        Ticket t = ticketDao.load( id );
        if ( t == null ) return null;
        initializeForProjection( t, includeEvents );
        return TicketValueObject.from( t, includeEvents );
    }

    /**
     * Force lazy init while the session is still open. Without this, the JAX-RS handler's
     * VO projection runs after the {@code @Transactional} ends and raises LazyInitializationException
     * ("no Session") on every lazy field — reporter (LAZY @ManyToOne), assignee, targets,
     * events, plus each event's actor.
     * <p>
     * Every read method below that hands entities back to the web layer runs this, because the
     * web layer's only use for them is {@link TicketValueObject#from}, which touches all of it.
     * The list paths pass {@code includeEvents=false}: list VOs deliberately omit the event log.
     */
    private void initializeForProjection( Ticket t, boolean includeEvents ) {
        if ( t.getReporter() != null ) Hibernate.initialize( t.getReporter() );
        if ( t.getAssignee() != null ) Hibernate.initialize( t.getAssignee() );
        Hibernate.initialize( t.getTargets() );
        if ( includeEvents ) {
            Hibernate.initialize( t.getEvents() );
            for ( TicketEvent e : t.getEvents() ) {
                if ( e.getActor() != null ) Hibernate.initialize( e.getActor() );
            }
        }
    }

    private <T extends List<Ticket>> T initializeForProjection( T tickets ) {
        for ( Ticket t : tickets ) {
            initializeForProjection( t, false );
        }
        return tickets;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        return initializeForProjection( ticketDao.findOpenForTarget( targetType, targetId ) );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findAssignedTo( Contact assignee ) {
        Assert.notNull( assignee, "Assignee cannot be null." );
        return initializeForProjection( ticketDao.findAssignedTo( assignee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority, int offset, int limit ) {
        return initializeForProjection( ticketDao.findTickets( openOnly, assigneeId, priority, offset, limit ) );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince, int offset, int limit ) {
        return initializeForProjection( ticketDao.findTickets( openOnly, assigneeId, priority, type, state, targetType, updatedSince, offset, limit ) );
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
        return initializeForProjection( ticketDao.findTicketsByCursor( openOnly, assigneeId, priority, cursor, limit ) );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince,
            @Nullable Cursor cursor, int limit ) {
        return initializeForProjection( ticketDao.findTicketsByCursor( openOnly, assigneeId, priority, type, state, targetType, updatedSince, cursor, limit ) );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Ticket> findOpenForTargetByCursor( TicketTargetType targetType, Long targetId,
            @Nullable Cursor cursor, int limit ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetId, "TargetId cannot be null." );
        return initializeForProjection( ticketDao.findOpenForTargetByCursor( targetType, targetId, cursor, limit ) );
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
