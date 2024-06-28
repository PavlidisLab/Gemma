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

import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The trail of events (create or update) that occurred in an objects lifetime. The first event added must be a "Create"
 * event, or an exception will be thrown.
 */
public class AuditTrail implements Identifiable, Serializable {

    private static final long serialVersionUID = -7450755789163303140L;
    private Long id;
    private List<AuditEvent> events = new ArrayList<>();

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AuditTrail ) ) {
            return false;
        }
        final AuditTrail that = ( AuditTrail ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    public List<AuditEvent> getEvents() {
        return this.events;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setEvents( List<AuditEvent> events ) {
        this.events = events;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return the first event in the audit trail.
     */
    @Transient
    public AuditEvent getCreationEvent() {
        assert this.getEvents() != null;
        if ( this.getEvents().size() == 0 ) {
            return null;
        }
        AuditEvent auditEvent = this.getEvents().get( 0 );

        assert auditEvent.getAction().equals( AuditAction.CREATE );

        return auditEvent;
    }

    /**
     * @return the last (most recent) event in the AuditTrail.
     */
    @Transient
    public AuditEvent getLast() {
        assert this.getEvents() != null;
        if ( this.getEvents().size() == 0 ) {
            return null;
        }
        return this.getEvents().get( this.getEvents().size() - 1 );
    }

    public static final class Factory {

        public static AuditTrail newInstance() {
            return new AuditTrail();
        }

    }

}