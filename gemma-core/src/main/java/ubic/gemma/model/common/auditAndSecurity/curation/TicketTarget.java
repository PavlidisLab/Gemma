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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ubic.gemma.model.common.AbstractIdentifiable;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * A single target of a {@link Ticket}. A ticket can have many targets, of
 * mixed types (Decision 2 of {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * <p>{@link #targetId} is a bare foreign key — intentionally NOT JPA-mapped
 * to a polymorphic association. The {@code (targetType, targetId)} composite
 * index supports the "open tickets for this entity" lookup without joining
 * back through {@code ticket}.</p>
 *
 * @author paul
 */
@Entity
@Table(name = "TICKET_TARGET")
public class TicketTarget extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TICKET_FK", nullable = false, columnDefinition = "BIGINT")
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_TYPE", nullable = false, columnDefinition = "VARCHAR(32)")
    private TicketTargetType targetType;

    @Column(name = "TARGET_ID", nullable = false, columnDefinition = "BIGINT")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, columnDefinition = "VARCHAR(16)")
    private TicketTargetStatus status = TicketTargetStatus.NOT_DONE;

    /**
     * The screening decision recorded for this target, or {@code null} when none has been.
     * Set independently of {@link #status} — see {@link ScreeningResult}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "SCREENING_RESULT", columnDefinition = "VARCHAR(16)")
    private ScreeningResult screeningResult;

    /**
     * Free-text explanation of {@link #screeningResult}, or {@code null}. The thing that makes
     * an {@link ScreeningResult#UNDECIDED} actionable to the next reader.
     */
    @Column(name = "SCREENING_RESULT_REASON", columnDefinition = "TEXT")
    private String screeningResultReason;

    /**
     * What this target's own task is — the finding that put THIS experiment on the ticket, and what the
     * curator is being asked to do about it.
     * <p>
     * The ticket's {@code body} and {@link Ticket#getPayload() payload} are one per ticket, so a ticket
     * carrying a thousand experiments can only say what is true of all thousand. Almost every finding an
     * audit produces is per-experiment, and a curator opening the 734th target needs that one
     * (frinkbro, 2026-09-11; Paul: "Each item has to have its task associated with it directly").
     * <p>
     * 🛑 Opaque to Gemma, exactly as {@link Ticket#getPayload()} is: nothing here parses, validates,
     * filters or indexes it, and the schema belongs to the producing agent.
     * <p>
     * Distinct from {@link #screeningResultReason}, which explains a screening verdict already reached.
     * <p>
     * The JDBC type is pinned rather than left to {@code @Lob}, which resolves to {@code Types#CLOB}
     * while Connector/J reports a MySQL {@code JSON} column as {@code Types#LONGVARCHAR} — the
     * disagreement that took gemma-staging (the one deployment running
     * {@code hbm2ddl.auto=validate}) down at startup on {@code ANNOTATION_SET.PAYLOAD_JSON}
     * (commit {@code 00eb15abc9}).
     */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Nullable
    @Column(name = "PAYLOAD", columnDefinition = "json")
    private String payload;

    /**
     * Which schema {@link #payload} follows, or null when the writer declared none — the per-target
     * counterpart of {@link Ticket#getPayloadSchemaVersion()}, and on the wire from the start for the
     * same reason.
     */
    @Nullable
    @Column(name = "PAYLOAD_SCHEMA_VERSION", columnDefinition = "INTEGER")
    private Integer payloadSchemaVersion;

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket( Ticket ticket ) {
        this.ticket = ticket;
    }

    public TicketTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType( TicketTargetType targetType ) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId( Long targetId ) {
        this.targetId = targetId;
    }

    public TicketTargetStatus getStatus() {
        return status;
    }

    public void setStatus( TicketTargetStatus status ) {
        this.status = status;
    }

    @Nullable
    public ScreeningResult getScreeningResult() {
        return screeningResult;
    }

    public void setScreeningResult( @Nullable ScreeningResult screeningResult ) {
        this.screeningResult = screeningResult;
    }

    @Nullable
    public String getScreeningResultReason() {
        return screeningResultReason;
    }

    public void setScreeningResultReason( @Nullable String screeningResultReason ) {
        this.screeningResultReason = screeningResultReason;
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

    @Override
    public int hashCode() {
        return Objects.hash( targetType, targetId );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof TicketTarget ) ) {
            return false;
        }
        TicketTarget that = ( TicketTarget ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return this.targetType == that.targetType
                && Objects.equals( this.targetId, that.targetId );
    }

    public static final class Factory {
        public static TicketTarget newInstance( TicketTargetType type, Long targetId ) {
            TicketTarget t = new TicketTarget();
            t.setTargetType( type );
            t.setTargetId( targetId );
            return t;
        }
    }
}
