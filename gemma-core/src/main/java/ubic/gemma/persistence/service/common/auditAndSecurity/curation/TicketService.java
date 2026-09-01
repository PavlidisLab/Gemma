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

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service-layer API for the Phase B-1 Ticket layer
 * (AUDIT_AS_WORKFLOW_RECCE.md). Mutating methods append a corresponding
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent} to
 * the ticket's event log; persistence is via JPA cascade.
 *
 * @author paul
 */
public interface TicketService extends BaseService<Ticket> {

    /**
     * Create a new ticket and seed its event log with a single
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType#OPENED}
     * event.
     *
     * @param reporter who is creating the ticket (must be non-null)
     * @param type     domain category
     * @param title    short human title; mandatory
     * @param targets  one or more targets; must contain at least one entry
     */
    Ticket openTicket( Contact reporter, TicketType type, String title, Collection<TicketTarget> targets );

    /**
     * Return the curator's scratchpad, provisioning it on first call.
     * <p>
     * A scratchpad is a {@link TicketType#SCRATCHPAD} ticket kept open indefinitely, holding whatever
     * the curator is currently looking at; finishing with a dataset means REMOVING it from the
     * scratchpad, not resolving the ticket (Paul, 2026-08-31). It is created with
     * {@code acceptsTargets = true}, because a scratchpad nothing can be added to is inert, and with
     * no targets, which is why this does not delegate to {@link #openTicket} (that method requires at
     * least one).
     * <p>
     * Identified by {@code type == SCRATCHPAD} and {@code reporter == curator}, with no state clause:
     * a cancelled scratchpad is still the curator's, and comes back as-is for them to reopen through
     * the normal state transition rather than being superseded by a fresh one.
     * <p>
     * 🛑 <b>Duplicate prevention is query-then-create inside one transaction, which is not a
     * guarantee.</b> Nothing in the schema forbids a second row, so two first-calls that both run the
     * SELECT before either commits will both insert. What IS guaranteed is that the identity never
     * splits afterwards: {@link TicketDao#findScratchpad} orders by {@code id} ascending and takes
     * one row, so every later call — this one included — returns the same ticket forever. A stray
     * duplicate is an orphan row visible in {@code GET /tickets?type=SCRATCHPAD} and reachable by id,
     * not a scratchpad that flips between two identities. Closing the window properly needs a unique
     * index, which needs a migration.
     * <p>
     * The returned ticket has its lazy fields initialized for
     * {@link TicketValueObject#from(Ticket, boolean)}, events included, so the REST layer can project
     * it after the transaction ends.
     *
     * @param curator the scratchpad's owner; recorded as the ticket's reporter
     */
    Ticket getOrCreateScratchpad( Contact curator );

    /**
     * Assign (or re-assign) the ticket. Appends an
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType#ASSIGNED}
     * event. Pass {@code assignee == null} to clear an assignment (still
     * appends an ASSIGNED event with a null target).
     */
    Ticket assign( Ticket ticket, Contact actor, @Nullable Contact assignee );

    /**
     * Append a comment event (free-form payload). Doesn't change ticket
     * state; doesn't change the {@code updatedAt} field beyond bumping
     * the audit timestamp.
     */
    Ticket addComment( Ticket ticket, Contact actor, @Nullable String payload );

    /**
     * Transition the ticket to a new state. Appends a
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType#STATE_CHANGED}
     * event (or one of the terminal-state aliases — RESOLVED / CANCELLED /
     * REOPENED — when the new state matches the corresponding terminal).
     * Bumps {@code updatedAt}.
     */
    Ticket transition( Ticket ticket, TicketState newState, Contact actor, @Nullable String reason );

    /**
     * Persist metadata-only edits (priority, dueDate, title, body, mode, etc.)
     * to a ticket. The caller mutates the {@link Ticket} arg in place, then
     * passes a comma-separated list of changed field names so the audit
     * trail row's NOTE column documents what changed.
     *
     * <p>Unlike {@link #transition}, {@link #assign}, {@link #addComment} —
     * which append rows to BOTH log streams — metadata edits write to the
     * governance {@code AuditTrail} stream ONLY (Decision 4 of
     * {@code AUDIT_AS_WORKFLOW_RECCE.md}: "no TicketEvent log spam for
     * fact-of-update edits"). Bumps {@code updatedAt}.
     */
    Ticket updateMetadata( Ticket ticket, String changedFields );

    /**
     * Update the status of a single {@link
     * ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget} on a
     * multi-target ticket. Typical agent flow: open ticket with N targets all
     * NOT_DONE, then call this method as each target completes (UNDERWAY ->
     * DONE). No-op if the target is already at {@code newStatus} (neither
     * stream is appended).
     *
     * <p>Appends a {@link
     * ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType#TARGET_STATUS_CHANGED}
     * row on the domain-workflow stream and a {@code
     * TicketTargetStatusChangedEvent} on the governance audit trail. Bumps
     * {@code updatedAt} on the ticket.
     *
     * @param ticket       the ticket whose target is being updated.
     * @param targetId     id of the {@code TicketTarget} row (NOT the
     *                     {@code targetId} field, which is the FK to the
     *                     external entity).
     * @param newStatus    the desired status.
     * @param actor        the contact performing the update (typically the
     *                     agent user for automated flows).
     * @throws IllegalArgumentException if no target with the given row id
     *                                  exists on this ticket.
     */
    /**
     * Add a target to a ticket that was already opened.
     * <p>
     * Until this existed a ticket's targets were fixed at {@link #openTicket}: the other target methods
     * only modify rows that are already there. The motivating case is a curator scratchpad — a ticket
     * someone keeps adding experiments to as they meet them.
     * <p>
     * Two conditions, both refused with {@link IllegalStateException}:
     * <ul>
     *   <li>the ticket's {@code acceptsTargets} flag must be set. It is false by default and false on
     *       every ticket predating the flag, so an agent-created ticket keeps the fixed batch it was
     *       opened for unless someone deliberately opens it up.</li>
     *   <li>the ticket must not be {@link TicketState#RESOLVED}, whatever the flag says, so a finished
     *       ticket cannot quietly grow new work. The flag is not rewritten — reopening the ticket makes
     *       it effective again.</li>
     * </ul>
     * A target already on the ticket is an {@link IllegalArgumentException}, matching how a duplicate
     * experiment tag is refused rather than silently ignored.
     *
     * @return the ticket with the new target attached
     */
    Ticket addTarget( Ticket ticket, TicketTargetType targetType, Long targetId, Contact actor );

    /**
     * Remove a target from a ticket.
     * <p>
     * On a curator scratchpad this is what finishing with a dataset looks like — the ticket stays open
     * and the dataset leaves it — so this is the counterpart of {@link #addTarget} rather than an
     * afterthought.
     * <p>
     * Idempotent: removing a target the ticket does not have returns null rather than throwing, since
     * the caller has already reached the state it asked for. A terminal ticket
     * ({@code RESOLVED} / {@code CANCELLED}) refuses the change.
     * <p>
     * Removing a target whose status is past {@code NOT_DONE} is permitted — a scratchpad's rows are
     * all NOT_DONE and refusing would make the common case pay for the rare one — so the removed
     * status is returned and the caller decides what to say about it.
     *
     * @return the status the removed target had, or {@code null} if it was not on the ticket
     */
    @Nullable
    TicketTargetStatus removeTarget( Ticket ticket, TicketTargetType targetType, Long targetId, Contact actor );

    Ticket updateTargetStatus( Ticket ticket, Long targetId,
            ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus newStatus, Contact actor );

    /**
     * Record a {@link ScreeningResult} on one target (by its row id). Uncoupled from
     * {@link #updateTargetStatus}: the two are set independently. No-op when unchanged;
     * writes a SCREENING_RESULT_CHANGED ticket event when it changes.
     */
    Ticket updateTargetScreeningResult( Ticket ticket, Long targetId,
            @Nullable ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult screeningResult,
            @Nullable String reason, boolean reasonProvided, Contact actor );

    /**
     * Load a ticket and project it to a {@link TicketValueObject} inside the same
     * transaction, force-initializing the {@code reporter} + {@code assignee} +
     * {@code targets} (and {@code events} + each event's {@code actor} when
     * {@code includeEvents=true}) so the projection doesn't raise
     * {@code LazyInitializationException} once the transaction ends and the
     * JAX-RS handler reads the returned VO.
     *
     * <p>Returns {@code null} when no ticket with that id exists; the REST
     * surface turns that into a 404.</p>
     */
    @Nullable
    TicketValueObject loadValueObject( Long id, boolean includeEvents );

    /** @see TicketDao#findOpenForTarget */
    List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId );

    /** @see TicketDao#findAssignedTo */
    List<Ticket> findAssignedTo( Contact assignee );

    /** @see TicketDao#findTickets(boolean, Long, TicketPriority, int, int) */
    List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority, int offset, int limit );

    /** @see TicketDao#findTickets(boolean, Long, TicketPriority, TicketType, TicketState, TicketTargetType, Date, int, int) */
    List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince, int offset, int limit );

    /** @see TicketDao#countTickets(boolean, Long, TicketPriority) */
    long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority );

    /** @see TicketDao#countTickets(boolean, Long, TicketPriority, TicketType, TicketState, TicketTargetType, Date) */
    long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince );

    /** @see TicketDao#findTicketsByCursor(boolean, Long, TicketPriority, Cursor, int) */
    CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable Cursor cursor, int limit );

    /** @see TicketDao#findTicketsByCursor(boolean, Long, TicketPriority, TicketType, TicketState, TicketTargetType, Date, Cursor, int) */
    CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince,
            @Nullable Cursor cursor, int limit );

    /** @see TicketDao#findOpenForTargetByCursor */
    CursorPage<Ticket> findOpenForTargetByCursor( TicketTargetType targetType, Long targetId,
            @Nullable Cursor cursor, int limit );

    /** @see TicketDao#findEventsByCursor */
    CursorPage<TicketEvent> findEventsByCursor( Ticket ticket, @Nullable Cursor cursor, int limit );

    /** @see TicketDao#countOpenByType */
    Map<TicketType, Long> countOpenByType();

    /** @see TicketDao#countOpen */
    long countOpen();

    /** @see TicketDao#findOldestOpenCreatedAt */
    @Nullable
    Date findOldestOpenCreatedAt();
}
