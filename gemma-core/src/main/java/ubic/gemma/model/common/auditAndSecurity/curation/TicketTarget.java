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

import ubic.gemma.model.common.AbstractIdentifiable;

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
public class TicketTarget extends AbstractIdentifiable {

    private Ticket ticket;
    private TicketTargetType targetType;
    private Long targetId;

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
