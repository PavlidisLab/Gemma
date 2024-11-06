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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Objects;

/**
 * An event in the life of an object.
 */
public class AuditEvent extends AbstractIdentifiable {

    private AuditAction action = null;
    private Date date = null;
    @Nullable
    private String detail = null;
    @Nullable
    private AuditEventType eventType = null;
    @Nullable
    private String note = null;
    @Nullable
    private User performer = null;

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
    }
}