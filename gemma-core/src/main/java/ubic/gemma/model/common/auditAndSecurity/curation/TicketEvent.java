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
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.util.Date;
import java.util.Objects;

/**
 * An append-only entry in a {@link Ticket}'s workflow event log. Mirrors the
 * Jackson payload shape used by
 * {@link ubic.gemma.model.common.auditAndSecurity.AuditEvent#getPayload()} so
 * a single {@code AuditEventPayload} record can populate either stream.
 *
 * <p>Phase B-1 is append-only (Decision 4 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}); edits aren't supported yet.</p>
 *
 * @author paul
 */
public class TicketEvent extends AbstractIdentifiable {

    private Ticket ticket;
    private Contact actor;
    private Date occurredAt = new Date();
    private TicketEventType type;

    /**
     * Raw JSON payload, same shape as
     * {@link ubic.gemma.model.common.auditAndSecurity.AuditEvent#getPayload()}.
     * Kept as an opaque string at the entity layer; callers that want typed
     * access decode with Jackson at the service layer.
     */
    @Nullable
    private String payload;

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket( Ticket ticket ) {
        this.ticket = ticket;
    }

    public Contact getActor() {
        return actor;
    }

    public void setActor( Contact actor ) {
        this.actor = actor;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt( Date occurredAt ) {
        this.occurredAt = occurredAt;
    }

    public TicketEventType getType() {
        return type;
    }

    public void setType( TicketEventType type ) {
        this.type = type;
    }

    @Nullable
    public String getPayload() {
        return payload;
    }

    public void setPayload( @Nullable String payload ) {
        this.payload = payload;
    }

    @Override
    public int hashCode() {
        return Objects.hash( type, occurredAt );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof TicketEvent ) ) {
            return false;
        }
        TicketEvent that = ( TicketEvent ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return this.type == that.type
                && Objects.equals( this.occurredAt, that.occurredAt );
    }

    public static final class Factory {
        public static TicketEvent newInstance( TicketEventType type, Contact actor, @Nullable String payload ) {
            TicketEvent e = new TicketEvent();
            e.setType( type );
            e.setActor( actor );
            e.setPayload( payload );
            return e;
        }
    }
}
