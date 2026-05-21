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

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The trail of events (create or update) that occurred in an objects lifetime. The first event added must be a "Create"
 * event, or an exception will be thrown.
 */
public class AuditTrail extends AbstractIdentifiable {

    private List<AuditEvent> events = new ArrayList<>();

    /**
     * Denormalised pointer to the most recent {@link AuditEvent} on this trail (by
     * {@code date} desc, {@code id} desc tie-breaker — same ordering the legacy
     * Java-side reducer used). Maintained by writers that append to {@link #events}
     * (currently {@link ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailServiceImpl#doAddUpdateEvent}
     * and {@link ubic.gemma.persistence.audit.AuditTrailEventListener#emitLifecycleEvent})
     * so that whole-corpus "last event of type T" queries (e.g. dashboard report
     * services on ExpressionExperiment) can JOIN through this FK rather than
     * pulling every audit row into the JVM and reducing in Java.
     * <p>
     * Nullable for freshly-persisted trails that have no events yet. ON DELETE SET
     * NULL on the FK so deleting an AuditEvent row doesn't break the trail.
     */
    @Nullable
    private AuditEvent lastEvent;

    public List<AuditEvent> getEvents() {
        return this.events;
    }

    public void setEvents( List<AuditEvent> events ) {
        this.events = events;
    }

    @Nullable
    public AuditEvent getLastEvent() {
        return this.lastEvent;
    }

    public void setLastEvent( @Nullable AuditEvent lastEvent ) {
        this.lastEvent = lastEvent;
    }

    /**
     * Append a new {@link AuditEvent} to this trail AND repoint
     * {@link #lastEvent} when the new event is more recent than the current
     * pointer under (date desc, id desc) ordering.
     * <p>
     * Centralised here so both production writers
     * ({@code AuditTrailServiceImpl#doAddUpdateEvent},
     * {@code AuditTrailEventListener#emitLifecycleEvent}) and test code stay
     * in sync. Direct {@code getEvents().add(...)} bypasses the
     * {@link #lastEvent} maintenance and is now a contract violation — the
     * whole-corpus {@code AuditEventDaoImpl#getLastEvents(Class, Class)} path
     * uses this denormalised FK and will return nothing for trails whose
     * pointer wasn't repointed.
     * <p>
     * Pre-flush note: when called before session flush, {@code event.getId()}
     * is still {@code null}. The tie-break branch in
     * {@link #maybeAdvanceLastEventOnAppend} treats a null-id candidate at an
     * equal-date tie as winning (the cascade insert will assign a strictly
     * larger id than any sibling).
     */
    public void addEvent( AuditEvent event ) {
        this.events.add( event );
        maybeAdvanceLastEventOnAppend( event );
    }

    private void maybeAdvanceLastEventOnAppend( AuditEvent candidate ) {
        AuditEvent current = this.lastEvent;
        if ( current == null ) {
            this.lastEvent = candidate;
            return;
        }
        Date currentDate = current.getDate();
        Date candidateDate = candidate.getDate();
        if ( candidateDate == null ) {
            return; // defensive — AuditEvent.date is NOT NULL in the mapping
        }
        if ( currentDate == null || candidateDate.after( currentDate ) ) {
            this.lastEvent = candidate;
            return;
        }
        if ( candidateDate.equals( currentDate ) ) {
            Long currentId = current.getId();
            Long candidateId = candidate.getId();
            // Pre-flush candidate id is null; the cascade insert assigns a
            // larger id than any sibling, so the candidate wins on tie.
            if ( candidateId == null || currentId == null || candidateId > currentId ) {
                this.lastEvent = candidate;
            }
        }
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