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

import java.util.ArrayList;
import java.util.List;

/**
 * The trail of events (create or update) that occurred in an objects lifetime. The first event added must be a "Create"
 * event, or an exception will be thrown.
 */
public class AuditTrail extends AbstractIdentifiable {

    private List<AuditEvent> events = new ArrayList<>();

    public List<AuditEvent> getEvents() {
        return this.events;
    }

    public void setEvents( List<AuditEvent> events ) {
        this.events = events;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AuditTrail ) ) {
            return false;
        }
        final AuditTrail that = ( AuditTrail ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        public static AuditTrail newInstance() {
            return new AuditTrail();
        }
    }
}