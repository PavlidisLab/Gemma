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

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A curation ticket targeting one or more entities (initially
 * {@link TicketTargetType#EXPRESSION_EXPERIMENT} and
 * {@link TicketTargetType#ARRAY_DESIGN}). Replaces the legacy
 * {@code CurationDetails} 1:1 model — see Decision 1 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}.
 *
 * <p>Tickets are themselves {@link ubic.gemma.model.common.auditAndSecurity.Auditable},
 * so two log streams coexist (Decision 6):</p>
 *
 * <ul>
 *   <li>{@link TicketEvent}s — domain workflow facts (state transitions,
 *       comments, assignments).</li>
 *   <li>Audit events on the inherited audit trail — governance who-touched-
 *       this-row facts.</li>
 * </ul>
 *
 * <p>The {@link #getName() name} property (inherited from
 * {@code AbstractDescribable}) holds the ticket {@code title}.</p>
 *
 * @author paul
 */
public class Ticket extends AbstractAuditable {

    private TicketType type;
    private TicketState state = TicketState.OPEN;
    private TicketPriority priority = TicketPriority.NORMAL;

    @Nullable
    private Date dueDate;

    private Contact reporter;

    @Nullable
    private Contact assignee;

    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    @Nullable
    private String externalIssueUrl;
    private ExternalIssueSyncState externalIssueSyncState = ExternalIssueSyncState.NONE;

    private Set<TicketTarget> targets = new HashSet<>();
    private Set<TicketEvent> events = new HashSet<>();

    public TicketType getType() {
        return type;
    }

    public void setType( TicketType type ) {
        this.type = type;
    }

    public TicketState getState() {
        return state;
    }

    public void setState( TicketState state ) {
        this.state = state;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority( TicketPriority priority ) {
        this.priority = priority;
    }

    @Nullable
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate( @Nullable Date dueDate ) {
        this.dueDate = dueDate;
    }

    /** The ticket title. Stored in the inherited {@code NAME} column. */
    public String getTitle() {
        return getName();
    }

    public void setTitle( String title ) {
        setName( title );
    }

    public Contact getReporter() {
        return reporter;
    }

    public void setReporter( Contact reporter ) {
        this.reporter = reporter;
    }

    @Nullable
    public Contact getAssignee() {
        return assignee;
    }

    public void setAssignee( @Nullable Contact assignee ) {
        this.assignee = assignee;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt( Date createdAt ) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt( Date updatedAt ) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public String getExternalIssueUrl() {
        return externalIssueUrl;
    }

    public void setExternalIssueUrl( @Nullable String externalIssueUrl ) {
        this.externalIssueUrl = externalIssueUrl;
    }

    public ExternalIssueSyncState getExternalIssueSyncState() {
        return externalIssueSyncState;
    }

    public void setExternalIssueSyncState( ExternalIssueSyncState externalIssueSyncState ) {
        this.externalIssueSyncState = externalIssueSyncState;
    }

    public Set<TicketTarget> getTargets() {
        return targets;
    }

    public void setTargets( Set<TicketTarget> targets ) {
        this.targets = targets;
    }

    public Set<TicketEvent> getEvents() {
        return events;
    }

    public void setEvents( Set<TicketEvent> events ) {
        this.events = events;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getName(), type, createdAt );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Ticket ) ) {
            return false;
        }
        Ticket that = ( Ticket ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return Objects.equals( this.getName(), that.getName() )
                && this.type == that.type
                && Objects.equals( this.createdAt, that.createdAt );
    }

    public static final class Factory {
        public static Ticket newInstance( TicketType type, String title, Contact reporter ) {
            Ticket t = new Ticket();
            t.setType( type );
            t.setTitle( title );
            t.setReporter( reporter );
            return t;
        }
    }
}
