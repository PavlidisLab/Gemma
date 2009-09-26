/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.OKStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.AuditableService
 */
public class AuditableServiceImpl extends ubic.gemma.model.common.AuditableServiceBase {

    /**
     * @param events
     * @param lastEvent
     * @return
     */
    protected AuditEvent getLastEvent( Collection<AuditEvent> events, Class<? extends AuditEventType> eventClass ) {
        AuditEvent lastEvent = null;
        if ( events == null ) return null;
        for ( AuditEvent event : events ) {
            if ( event.getEventType() != null && eventClass.isAssignableFrom( event.getEventType().getClass() ) ) {
                if ( lastEvent == null ) {
                    lastEvent = event; // first one.
                    continue;
                } else if ( lastEvent.getDate().before( event.getDate() ) ) {
                    lastEvent = event;
                }
            }
        }
        return lastEvent;
    }

    /**
     * @param events
     * @return
     */
    protected AuditEvent getLastOutstandingTroubleEvent( Collection<AuditEvent> events ) {
        return getLastOutstandingTroubleEventNoSort( events );
    }

    /**
     * @see ubic.gemma.model.common.AuditableService#getEvents(ubic.gemma.model.common.Auditable)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection<AuditEvent> handleGetEvents( ubic.gemma.model.common.Auditable auditable )
            throws java.lang.Exception {
        Collection<AuditEvent> auditEvents = this.getAuditableDao().getAuditEvents( auditable );
        return auditEvents;
    }

    /**
     * @see ubic.gemma.model.common.AuditableService#getLastAuditEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    @Override
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( final Auditable auditable,
            AuditEventType type ) throws java.lang.Exception {
        return this.getAuditableDao().getLastAuditEvent( auditable, type );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.AuditableServiceBase#handleGetLastAuditEvent(java.util.Collection,
     * ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    @Override
    protected Map handleGetLastAuditEvent( Collection<? extends Auditable> auditables, AuditEventType type ) throws Exception {
        return this.getAuditableDao().getLastAuditEvent( auditables, type );
    }

  

    /**
     * @param events
     * @return
     */
    private AuditEvent getLastOutstandingTroubleEventNoSort( Collection<AuditEvent> events ) {
        AuditEvent lastTroubleEvent = null;
        AuditEvent lastOKEvent = null;
        for ( AuditEvent event : events ) {
            if ( event.getEventType() == null ) {
                continue;
            } else if ( OKStatusFlagEvent.class.isAssignableFrom( event.getEventType().getClass() ) ) {
                if ( lastOKEvent == null || lastOKEvent.getDate().before( event.getDate() ) ) lastOKEvent = event;
            } else if ( TroubleStatusFlagEvent.class.isAssignableFrom( event.getEventType().getClass() ) ) {
                if ( lastTroubleEvent == null || lastTroubleEvent.getDate().before( event.getDate() ) )
                    lastTroubleEvent = event;
            }
        }
        if ( lastTroubleEvent != null )
            if ( lastOKEvent == null || lastOKEvent.getDate().before( lastTroubleEvent.getDate() ) )
                return lastTroubleEvent;
        return null;
    }
}