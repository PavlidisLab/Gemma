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

import java.util.Collection;

/**
 * The trail of events (create or update) that occurred in an objects lifetime. The first event added must be a "Create"
 * event, or an exception will be thrown.
 */
public abstract class AuditTrail implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -7450755789163303140L;

    /**
     * Constructs new instances of {@link AuditTrail}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link AuditTrail}.
         */
        public static AuditTrail newInstance() {
            return new AuditTrailImpl();
        }

    }

    private Long id;

    private Collection<AuditEvent> events = new java.util.ArrayList<AuditEvent>();

    /**
     * Add an event to the AuditTrail
     */
    public abstract void addEvent( AuditEvent event );

    /**
     * Returns <code>true</code> if the argument is an AuditTrail instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AuditTrail ) ) {
            return false;
        }
        final AuditTrail that = ( AuditTrail ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * Get the first event in the audit trail.
     */
    public abstract AuditEvent getCreationEvent();

    /**
     * 
     */
    public Collection<AuditEvent> getEvents() {
        return this.events;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Get the last (most recent) event in the AuditTrail.
     */
    public abstract AuditEvent getLast();

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    /**
     * 
     */
    public abstract void read();

    /**
     * 
     */
    public abstract void read( String note );

    /**
     * 
     */
    public abstract void read( String note, User actor );

    public void setEvents( Collection<AuditEvent> events ) {
        this.events = events;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * 
     */
    public abstract void start();

    /**
     * 
     */
    public abstract void start( String note );

    /**
     * 
     */
    public abstract void start( String note, User actor );

    /**
     * 
     */
    public abstract void update();

    /**
     * 
     */
    public abstract void update( String note );

    /**
     * 
     */
    public abstract void update( String note, User actor );

}