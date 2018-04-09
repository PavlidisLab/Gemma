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

import gemma.gsec.model.User;
import org.apache.commons.lang3.reflect.FieldUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.io.Serializable;
import java.util.Date;

/**
 * An event in the life of an object.
 */
public class AuditEvent implements Identifiable, Serializable {

    private static final long serialVersionUID = -1212093157703833905L;
    private final AuditAction action = null;
    private final Date date = null;
    private final String detail = null;
    private final AuditEventType eventType = null;
    private final Long id = null;
    private final String note = null;
    private final User performer = null;

    @Override
    public int hashCode() {
        int hashCode = 0;
        //noinspection ConstantConditions // Hibernate populates id through reflection
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );
        return hashCode;
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
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    public AuditAction getAction() {
        return this.action;
    }

    public Date getDate() {
        return this.date;
    }

    public String getDetail() {
        return this.detail;
    }

    public AuditEventType getEventType() {
        return this.eventType;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public String getNote() {
        return this.note;
    }

    public User getPerformer() {
        return this.performer;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static AuditEvent newInstance() {
            return new AuditEvent();
        }

        public static AuditEvent newInstance( Date date, AuditAction action, String note, String detail, User performer,
                AuditEventType eventType ) {
            final AuditEvent entity = new AuditEvent();
            try {
                if ( date != null )
                    FieldUtils.writeField( entity, "date", date, true );
                if ( action != null )
                    FieldUtils.writeField( entity, "action", action, true );
                if ( note != null )
                    FieldUtils.writeField( entity, "note", note, true );
                if ( detail != null )
                    FieldUtils.writeField( entity, "detail", detail, true );
                if ( performer != null )
                    FieldUtils.writeField( entity, "performer", performer, true );
                if ( eventType != null )
                    FieldUtils.writeField( entity, "eventType", eventType, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            return entity;
        }
    }

}