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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;
import java.util.List;

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

    /** @see TicketDao#findOpenForTarget */
    List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId );

    /** @see TicketDao#findAssignedTo */
    List<Ticket> findAssignedTo( Contact assignee );

    /** @see TicketDao#findTickets */
    List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority, int offset, int limit );

    /** @see TicketDao#countTickets */
    long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority );
}
