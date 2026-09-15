/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.model.common.auditAndSecurity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import org.springframework.lang.Nullable;
import java.util.Date;
import java.util.Objects;

/**
 * An event in the life of an object.
 */
@Entity
@Table(name = "AUDIT_EVENT", indexes = {
        @Index(name = "AUDIT_EVENT_DATE", columnList = "DATE"),
        @Index(name = "AUDIT_EVENT_ACTION", columnList = "ACTION")
})
public class AuditEvent extends AbstractIdentifiable {

    public static final int
            MAX_NOTE_LENGTH = 65535,
            MAX_DETAIL_LENGTH = 65535;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION", nullable = false, columnDefinition = "VARCHAR(255)")
    private AuditAction action = null;

    @Column(name = "DATE", nullable = false, columnDefinition = "DATETIME(3)")
    private Date date = null;

    @Lob
    @Nullable
    @Column(name = "DETAIL", columnDefinition = "text")
    private String detail = null;

    // we cannot use component mapping here because of the polymorphism of auditeventtypes see HHH1152
    @Nullable
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "EVENT_TYPE_FK", columnDefinition = "BIGINT", unique = true)
    private AuditEventType eventType = null;

    @Lob
    @Nullable
    @Column(name = "NOTE", columnDefinition = "text")
    private String note = null;

    @Nullable
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PERFORMER_FK", columnDefinition = "BIGINT")
    private User performer = null;

    /**
     * The curator this action was taken FOR, when the {@link #performer} was carrying it for someone
     * else. {@code null} on the ordinary case, where the performer IS the actor.
     * <p>
     * The two are different facts and both are worth keeping. An agent authenticated as
     * {@code gemmaAgent} committing a curator's draft is a true statement about which credential wrote
     * the row and a useless answer to "who decided this"; recording only the curator would be the
     * reverse, and lose which key was used. So the performer stays the credential and this names the
     * human.
     * <p>
     * 🛑 A name, not an FK to {@code CONTACT}, and deliberately — the same reason
     * {@code CURATION_LOCK.LOCKED_BY} and {@code AnnotationSet.createdBy} are. An FK makes the row
     * un-writable for any identity without a Gemma account, and an audit write that fails takes the
     * commit down with it. A curator who is not a Gemma user is exactly the case this has to survive.
     */
    @Nullable
    @Column(name = "ON_BEHALF_OF", columnDefinition = "VARCHAR(255)")
    private String onBehalfOf = null;

    /**
     * Raw JSON serialisation of an {@link
     * ubic.gemma.core.security.audit.AuditEventPayload} record, when the
     * originating {@link ubic.gemma.core.security.audit.Audited}-annotated
     * service method declared a payload parameter. Kept as an unparsed string
     * at the entity level; callers that want typed access should use
     * {@code objectMapper.readValue(payload, AuditEventPayload.class)}.
     * Phase A of {@code AUDIT_SYSTEM_AUDIT.md}.
     */
    @Lob
    @Nullable
    @Column(name = "PAYLOAD", columnDefinition = "json")
    private String payload = null;

    @Override
    public int hashCode() {
        return Objects.hash( action, date, detail, eventType, note, performer );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AuditEvent ) ) {
            return false;
        }
        final AuditEvent that = ( AuditEvent ) object;

        //noinspection ConstantConditions // Hibernate populates id through reflection
        return !( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) );
    }

    @Override
    public String toString() {
        return String.format( "%s Action=%s Date=%s Performer=%s EventType=%s Note=%s Detail=%s",
                super.toString(), action, date, performer, eventType, note, detail );
    }


    public AuditAction getAction() {
        return this.action;
    }

    public Date getDate() {
        return this.date;
    }

    @Nullable
    public String getDetail() {
        return this.detail;
    }

    @Nullable
    public AuditEventType getEventType() {
        return this.eventType;
    }

    @Nullable
    public String getNote() {
        return this.note;
    }

    @Nullable
    public User getPerformer() {
        return this.performer;
    }

    /**
     * @return the raw JSON serialisation of the originating
     * {@link ubic.gemma.core.security.audit.AuditEventPayload}, or {@code null}
     * for legacy / payload-less events.
     */
    @Nullable
    public String getPayload() {
        return this.payload;
    }

    /**
     * Set the raw JSON payload string. Intentionally package-private; used by
     * {@code AuditedAspect} immediately after factory instantiation and by
     * Hibernate field-access mapping. Do not call from application code —
     * AuditEvent is otherwise still immutable.
     */
    void setPayload( @Nullable String payload ) {
        this.payload = payload;
    }

    /**
     * @return the curator this action was taken for, or {@code null} when the {@link #getPerformer()
     * performer} was acting for themselves — which is the ordinary case and most rows.
     */
    @Nullable
    public String getOnBehalfOf() {
        return this.onBehalfOf;
    }

    /** Package-private for the same reason {@link #setPayload} is: set once, at creation. */
    void setOnBehalfOf( @Nullable String onBehalfOf ) {
        this.onBehalfOf = onBehalfOf;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {
        /**
         * Create a new, immutable audit event.
         */
        public static AuditEvent newInstance( Date date, AuditAction action, String note, String detail, User performer,
                AuditEventType eventType ) {
            AuditEvent entity = new AuditEvent();
            entity.date = date;
            entity.action = action;
            entity.note = note;
            entity.detail = detail;
            entity.performer = performer;
            entity.eventType = eventType;
            return entity;
        }

        /**
         * Create a new audit event with a serialised payload. Phase A of
         * {@code AUDIT_SYSTEM_AUDIT.md}: used by {@code AuditedAspect} when the
         * annotated method declared an {@link ubic.gemma.core.security.audit.AuditEventPayload}
         * parameter.
         */
        public static AuditEvent newInstance( Date date, AuditAction action, String note, String detail, User performer,
                AuditEventType eventType, @Nullable String payload ) {
            AuditEvent entity = newInstance( date, action, note, detail, performer, eventType );
            entity.payload = payload;
            return entity;
        }

        /**
         * As above, naming the curator the action was taken for. See {@link AuditEvent#getOnBehalfOf()}.
         */
        public static AuditEvent newInstance( Date date, AuditAction action, String note, String detail, User performer,
                AuditEventType eventType, @Nullable String payload, @Nullable String onBehalfOf ) {
            AuditEvent entity = newInstance( date, action, note, detail, performer, eventType, payload );
            entity.onBehalfOf = onBehalfOf;
            return entity;
        }
    }
}
