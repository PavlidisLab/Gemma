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
