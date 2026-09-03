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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
@Entity
@Table(name = "TICKET", indexes = {
        @Index(name = "TICKET_NAME", columnList = "NAME"),
        @Index(name = "TICKET_TYPE", columnList = "TYPE"),
        @Index(name = "TICKET_STATE", columnList = "STATE")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@AttributeOverride(name = "name", column = @Column(name = "NAME", nullable = false, columnDefinition = "VARCHAR(255)"))
public class Ticket extends AbstractAuditable {

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, columnDefinition = "VARCHAR(64)")
    private TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false, columnDefinition = "VARCHAR(32)")
    private TicketState state = TicketState.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORITY", nullable = false, columnDefinition = "VARCHAR(16)")
    private TicketPriority priority = TicketPriority.NORMAL;

    @Nullable
    @Column(name = "DUE_DATE", columnDefinition = "DATETIME(3)")
    private Date dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTER_FK", nullable = false, columnDefinition = "BIGINT")
    private Contact reporter;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNEE_FK", columnDefinition = "BIGINT")
    private Contact assignee;

    @Column(name = "CREATED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date createdAt = new Date();

    @Column(name = "UPDATED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date updatedAt = new Date();

    @Nullable
    @Column(name = "EXTERNAL_ISSUE_URL", columnDefinition = "VARCHAR(512)")
    private String externalIssueUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXTERNAL_ISSUE_SYNC_STATE", nullable = false, columnDefinition = "VARCHAR(16)")
    private ExternalIssueSyncState externalIssueSyncState = ExternalIssueSyncState.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODE", nullable = false, columnDefinition = "VARCHAR(16)")
    private TicketMode mode = TicketMode.MANUAL;

    /**
     * Whether experiments may be added to this ticket after it was opened.
     * <p>
     * A ticket's targets are otherwise fixed at creation: {@code openTicket} takes the collection and
     * nothing adds to it afterwards. This flag turns a ticket into an open list — the motivating case
     * being a curator scratchpad, a ticket someone keeps adding experiments to as they meet them rather
     * than one describing a fixed batch of work.
     * <p>
     * False by default, and every ticket that predates the flag is false, so the agent-created tickets
     * keep the fixed target list they were opened for.
     * <p>
     * 🛑 Necessary but not sufficient: a {@link TicketState#RESOLVED} ticket refuses additions whatever
     * this says, so a finished ticket cannot quietly grow new work. That rule lives in the service so
     * that reopening a ticket restores the flag's effect without rewriting the flag itself.
     */
    @Column(name = "ACCEPTS_TARGETS", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean acceptsTargets = false;

    /**
     * What the screen that produced this ticket asked — its summary, the window it scraped, the verbs on its
     * buttons, and the fields the producing agent wants shown per candidate.
     * <p>
     * 🛑 Opaque to Gemma, exactly as {@code Investigation.sourceMetadata} is: the schema belongs to the agents
     * repo and nothing here parses, validates, filters or indexes it. Title, body and targets say which
     * experiments need work; this says what question was put to the curator, which no other field can carry —
     * {@code body} is a string and {@link TicketEvent#getPayload()} is per-event.
     * <p>
     * Set at creation. A screen's definition describes the question that was asked, so rewriting it later
     * would rewrite what curators were shown; edits are deliberately not offered.
     */
    @Lob
    @Nullable
    @Column(name = "PAYLOAD", columnDefinition = "json")
    private String payload;

    /**
     * Which schema {@link #payload} follows, or null when the writer declared none.
     * <p>
     * Ships with the payload rather than after it. {@code Investigation.sourceMetadata} had a version column
     * from the start and did not serialize it, which left a consumer holding a blob unable to tell one
     * document shape from another except by guessing at its keys (uib, 2026-09-03). Null is a real answer.
     */
    @Nullable
    @Column(name = "PAYLOAD_SCHEMA_VERSION", columnDefinition = "INTEGER")
    private Integer payloadSchemaVersion;

    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TicketTarget> targets = new HashSet<>();

    // Mirrors AuditTrail.events shape (List + @OrderBy) so iteration order
    // reflects the workflow timeline. JPA @OrderBy is silent on a Set
    // (HashSet ignores SQL ORDER BY); using List makes it actually honoured.
    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt")
    private List<TicketEvent> events = new ArrayList<>();

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

    /**
     * Curator-facing instructions text — what the reporter writes when filing the ticket,
     * what the detail page renders as multi-line body and the dashboard clamps to 2 lines.
     * Stored in the inherited {@code DESCRIPTION} column; this is a convenience alias
     * parallel to {@link #getTitle()} / {@link #setTitle(String)}.
     */
    @Nullable
    public String getBody() {
        return getDescription();
    }

    public void setBody( @Nullable String body ) {
        setDescription( body );
    }

    public TicketMode getMode() {
        return mode;
    }

    public boolean isAcceptsTargets() {
        return acceptsTargets;
    }

    public void setAcceptsTargets( boolean acceptsTargets ) {
        this.acceptsTargets = acceptsTargets;
    }

    public void setMode( TicketMode mode ) {
        this.mode = mode;
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

    public List<TicketEvent> getEvents() {
        return events;
    }

    public void setEvents( List<TicketEvent> events ) {
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

    @Nullable
    public String getPayload() {
        return payload;
    }

    public void setPayload( @Nullable String payload ) {
        this.payload = payload;
    }

    @Nullable
    public Integer getPayloadSchemaVersion() {
        return payloadSchemaVersion;
    }

    public void setPayloadSchemaVersion( @Nullable Integer payloadSchemaVersion ) {
        this.payloadSchemaVersion = payloadSchemaVersion;
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
