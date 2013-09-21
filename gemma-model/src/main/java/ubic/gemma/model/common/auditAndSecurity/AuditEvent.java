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

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.reflect.FieldUtils;

import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * An event in the life of an object.
 */
public abstract class AuditEvent implements Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.auditAndSecurity.AuditEvent}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.auditAndSecurity.AuditEvent}.
         */
        public static AuditEvent newInstance() {
            return new AuditEventImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.auditAndSecurity.AuditEvent}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static AuditEvent newInstance( Date date, AuditAction action, String note, String detail,
                User performer, AuditEventType eventType ) {
            final AuditEvent entity = new AuditEventImpl();
            try {
                if ( date != null ) FieldUtils.writeField( entity, "date", date, true );
                if ( action != null ) FieldUtils.writeField( entity, "action", action, true );
                if ( note != null ) FieldUtils.writeField( entity, "note", note, true );
                if ( detail != null ) FieldUtils.writeField( entity, "detail", detail, true );
                if ( performer != null ) FieldUtils.writeField( entity, "performer", performer, true );
                if ( eventType != null ) FieldUtils.writeField( entity, "eventType", eventType, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6713721089643871509L;

    private final AuditAction action = null;

    private final Date date = null;

    private final String detail = null;

    private final AuditEventType eventType = null;

    private final Long id = null;

    private final String note = null;

    private final User performer = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public AuditEvent() {
    }

    /**
     * Returns <code>true</code> if the argument is an AuditEvent instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AuditEvent ) ) {
            return false;
        }
        final AuditEvent that = ( AuditEvent ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditAction getAction() {
        return this.action;
    }

    /**
     * 
     */
    public java.util.Date getDate() {
        return this.date;
    }

    /**
     * 
     */
    public String getDetail() {
        return this.detail;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType getEventType() {
        return this.eventType;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * An annotation about the action taken.
     */
    public String getNote() {
        return this.note;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.User getPerformer() {
        return this.performer;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public abstract String toString();

}