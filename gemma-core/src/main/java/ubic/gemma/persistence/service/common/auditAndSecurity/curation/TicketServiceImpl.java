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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketSearchHitValueObject;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        return create( reporter, type, title, null, targets, false );
    }

    @Override
    @Transactional
    public Ticket getOrCreateScratchpad( Contact curator ) {
        Assert.notNull( curator, "Curator cannot be null." );
        Ticket existing = ticketDao.findScratchpad( curator );
        if ( existing != null ) {
            initializeForProjection( existing, true );
            return existing;
        }
        // 🛑 Query-then-create. Two first-calls that both miss will both insert, because no unique
        // index exists to stop them. The consequence is bounded rather than prevented: findScratchpad
        // is oldest-id-wins, so a duplicate is a stray row and never a second identity. See
        // TicketService#getOrCreateScratchpad.
        //
        // Not routed through openTicket: that method requires at least one target and a fresh
        // scratchpad has none. The shared create() below is the same path minus that check.
        Ticket created = create( curator, TicketType.SCRATCHPAD, scratchpadTitle( curator ),
                SCRATCHPAD_BODY, Collections.emptyList(), true );
        initializeForProjection( created, true );
        return created;
    }

    /**
     * Rendered by the ticket detail page like any other body. It says what the scratchpad is and how
     * it is finished with, so the ticket explains itself where it turns up in a generic ticket list.
     */
    private static final String SCRATCHPAD_BODY = "Datasets you are currently working on."
            + " Remove one when you are finished with it; the scratchpad itself stays open.";

    /** {@code Scratchpad: alice}, or plain {@code Scratchpad} for a contact with no name. */
    private static String scratchpadTitle( Contact curator ) {
        String name = curator.getName();
        return name != null && !name.trim().isEmpty() ? "Scratchpad: " + name.trim() : "Scratchpad";
    }

    /**
     * The one create path: seed the timestamps, attach the targets, append the OPENED event, persist,
     * and write the companion AuditTrail row. {@link #openTicket} adds the at-least-one-target check
     * on top; {@link #getOrCreateScratchpad} deliberately does not.
     */
    private Ticket create( Contact reporter, TicketType type, String title, @Nullable String body,
            Collection<TicketTarget> targets, boolean acceptsTargets ) {
        Ticket t = Ticket.Factory.newInstance( type, title, reporter );
        Date now = new Date();
        t.setCreatedAt( now );
        t.setUpdatedAt( now );
        t.setBody( body );
        t.setAcceptsTargets( acceptsTargets );
        for ( TicketTarget tgt : targets ) {
            tgt.setTicket( t );
            t.getTargets().add( tgt );
        }
        appendEvent( t, TicketEventType.OPENED, reporter, null );
        Ticket created = ticketDao.create( t );
        // @Audited targets the first Auditable method argument; the create path
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
    public TicketService.TargetAddition addTarget( Ticket ticket, TicketTargetType targetType, Long targetId, Contact actor ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( targetType, "targetType cannot be null." );
        Assert.notNull( targetId, "targetId cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        Ticket attached = reattach( ticket );

        if ( !attached.isAcceptsTargets() ) {
            throw new IllegalStateException( "Ticket " + attached.getId()
                    + " does not accept added targets. Its targets were fixed when it was opened;"
                    + " set acceptsTargets to open it up." );
        }
        // State wins over the flag: a finished ticket cannot quietly grow new work. The flag is left
        // alone, so reopening the ticket makes it effective again.
        if ( isTerminal( attached.getState() ) ) {
            throw new IllegalStateException( "Ticket " + attached.getId() + " is " + attached.getState()
                    + " and does not accept added targets. Reopen it first." );
        }
        // Idempotent, not a conflict: a caller cannot know current membership at click time, and
        // clicking twice must not error for reaching the state it asked for (uib, 2026-08-31).
        for ( TicketTarget existing : attached.getTargets() ) {
            if ( existing.getTargetType() == targetType && targetId.equals( existing.getTargetId() ) ) {
                return new TicketService.TargetAddition( attached, false );
            }
        }

        TicketTarget tgt = new TicketTarget();
        tgt.setTargetType( targetType );
        tgt.setTargetId( targetId );
        tgt.setTicket( attached );
        attached.getTargets().add( tgt );
        bumpUpdated( attached );

        String summary = "added target " + targetId + " (" + targetType + ")";
        appendEvent( attached, TicketEventType.TARGET_ADDED, actor, summary );
        Ticket saved = ticketDao.save( attached );
        // 🛑 Reuses TicketMetadataChangedEvent rather than introducing a TicketTargetAddedEvent.
        // Gemma 1.0 (1.32.8) carries the five ticket AuditEventType classes but has no Ticket entity,
        // and a sixth type it does not know would break its audit-trail reads -- the regression that
        // backport was made to fix. The summary carries what actually happened.
        auditTrailService.addUpdateEvent( saved, TicketMetadataChangedEvent.class, summary );
        return new TicketService.TargetAddition( saved, true );
    }

    @Override
    @Transactional
    public TicketTargetStatus removeTarget( Ticket ticket, TicketTargetType targetType, Long targetId, Contact actor ) {
        Assert.notNull( ticket, "Ticket cannot be null." );
        Assert.notNull( targetType, "targetType cannot be null." );
        Assert.notNull( targetId, "targetId cannot be null." );
        Assert.notNull( actor, "Actor cannot be null." );
        Ticket attached = reattach( ticket );

        if ( isTerminal( attached.getState() ) ) {
            throw new IllegalStateException( "Ticket " + attached.getId() + " is " + attached.getState()
                    + " and cannot have targets removed. Reopen it first." );
        }
        TicketTarget found = null;
        for ( TicketTarget t : attached.getTargets() ) {
            if ( t.getTargetType() == targetType && targetId.equals( t.getTargetId() ) ) {
                found = t;
                break;
            }
        }
        // Idempotent, same reasoning as addTarget: removing something that is not there has already
        // reached the state the caller asked for. Null tells the route to answer 204.
        if ( found == null ) {
            return null;
        }
        // 🛑 Deliberately NOT refused when the target is past NOT_DONE. A scratchpad's rows are all
        // NOT_DONE and blocking would make the common case pay for the rare one; the status is returned
        // instead so the caller can say what it discarded and decide whether to have prompted.
        TicketTargetStatus removedStatus = found.getStatus();
        attached.getTargets().remove( found );
        found.setTicket( null );
        bumpUpdated( attached );

        String summary = "removed target " + targetId + " (" + targetType + ", was " + removedStatus + ")";
        appendEvent( attached, TicketEventType.TARGET_REMOVED, actor, summary );
        Ticket saved = ticketDao.save( attached );
        // Same reuse rationale as addTarget: no new AuditEventType, which Gemma 1.0 would not know.
        auditTrailService.addUpdateEvent( saved, TicketMetadataChangedEvent.class, summary );
        return removedStatus;
    }

    /** A finished ticket takes no target changes, whichever way it was finished. */
    private static boolean isTerminal( TicketState state ) {
        return state == TicketState.RESOLVED || state == TicketState.CANCELLED;
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

    /**
     * The event-page counterpart. A page of {@link TicketEvent} carries no Ticket to walk, and the
     * one lazy field on an event is its actor — which {@link TicketEventValueObject#from} reads for
     * every row, after this transaction has closed.
     */
    private <T extends List<TicketEvent>> T initializeEventsForProjection( T events ) {
        for ( TicketEvent e : events ) {
            if ( e.getActor() != null ) Hibernate.initialize( e.getActor() );
        }
        return events;
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
    public Map<Long, List<TicketSearchHitValueObject>> findOpenSummariesForTargets( TicketTargetType targetType,
            Collection<Long> targetIds ) {
        Assert.notNull( targetType, "TargetType cannot be null." );
        Assert.notNull( targetIds, "TargetIds cannot be null." );
        // No initializeForProjection: the DAO projects scalars, so nothing lazy crosses the transaction
        // boundary. That is the point of the VO — no reporter, assignee, targets or events to fetch.
        return ticketDao.findOpenSummariesForTargets( targetType, targetIds );
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
        return initializeEventsForProjection( ticketDao.findEventsByCursor( ticket, cursor, limit ) );
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

    @Override
    @Transactional(readOnly = true)
    public List<TicketSearchHitValueObject> searchTickets( String query, boolean openOnly,
            @Nullable Long callerContactId, int limit ) {
        Assert.notNull( query, "Query cannot be null." );
        Assert.isTrue( limit > 0, "Limit must be greater than zero." );
        String trimmed = query.trim();
        if ( trimmed.isEmpty() ) {
            return Collections.emptyList();
        }
        List<TicketSearchHitValueObject> hits = new ArrayList<>( limit );
        Long typedId = parseTicketId( trimmed );
        if ( typedId != null ) {
            // Looked up on its own rather than folded into the title query's ORDER BY: a ticket
            // named by number can be older than the `limit` most recently touched title matches,
            // and a hit pushed off the end of that window looks exactly like one that does not
            // exist. A null here IS "no such ticket" — a non-hit, not a 404 for the caller.
            TicketSearchHitValueObject byId = ticketDao.findSearchHitById( typedId, openOnly, callerContactId );
            if ( byId != null ) {
                hits.add( byId );
            }
        }
        if ( hits.size() < limit ) {
            for ( TicketSearchHitValueObject hit : ticketDao.findSearchHitsByTitle( trimmed, openOnly, callerContactId, limit ) ) {
                if ( typedId != null && typedId.equals( hit.getId() ) ) {
                    continue; // already listed first; a ticket titled "6" would otherwise appear twice
                }
                hits.add( hit );
                if ( hits.size() == limit ) {
                    break;
                }
            }
        }
        return hits;
    }

    /**
     * Read the picker's contents as a ticket id, or {@code null} when they are not one.
     * <p>
     * Verbatim means verbatim: digits, nothing else. {@code "6"} is ticket 6; {@code "6 samples"},
     * {@code "#6"}, {@code "-6"} and {@code " 6.0"} are title text and go only to the title query.
     * Anything longer than 18 digits is title text too rather than a parse failure.
     */
    @Nullable
    private static Long parseTicketId( String s ) {
        if ( s.isEmpty() || s.length() > 18 ) {
            return null;
        }
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            if ( c < '0' || c > '9' ) {
                return null;
            }
        }
        return Long.valueOf( s );
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
