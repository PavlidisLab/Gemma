/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.OKStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailServiceImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase {

    private static Log log = LogFactory.getLog( AuditTrailServiceImpl.class.getName() );

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#audit(ubic.gemma.model.common.Describable,
     *      ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    @Override
    protected void handleAudit( Auditable entity, ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent )
            throws java.lang.Exception {

        if ( entity == null || entity.getId() == null ) return;

        AuditTrail at = entity.getAuditTrail();
        if ( at == null ) {
            at = AuditTrail.Factory.newInstance();
            at.start(); // uh-oh, have to update the entity. Hard to do from here.
            if ( auditEvent != null ) at.addEvent( auditEvent ); // should we do that? I guess so.
            this.getAuditTrailDao().create( at );
            log.warn( "Creating new audit trail for " + entity );
        } else {
            if ( auditEvent == null ) throw new IllegalArgumentException( "auditEvent cannot be null" );
            at.addEvent( auditEvent );
            this.getAuditTrailDao().update( at );
            log.debug( "Added event " + auditEvent.getAction() + " to " + entity );
        }
    }

    /**
     * @param auditTrail
     * @return
     */
    @Override
    protected AuditTrail handleCreate( AuditTrail auditTrail ) {
        return this.getAuditTrailDao().create( auditTrail );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase#handleThaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    @Override
    protected void handleThaw( AuditTrail auditTrail ) throws Exception {
        this.getAuditTrailDao().thaw( auditTrail );

    }

    @Override
    protected void handleThaw( Auditable auditable ) throws Exception {
        this.getAuditTrailDao().thaw( auditable );

    }

    @Override
    protected AuditEvent handleAddUpdateEvent( Auditable auditable, String note ) throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( Calendar.getInstance().getTime() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setNote( note );
        return this.getAuditTrailDao().addEvent( auditable, auditEvent );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase#handleAddUpdateEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType, java.lang.String)
     */
    @Override
    protected AuditEvent handleAddUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note )
            throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( Calendar.getInstance().getTime() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setEventType( auditEventType );
        auditEvent.setNote( note );
        return this.getAuditTrailDao().addEvent( auditable, auditEvent );
    }
    
    @Override
    protected AuditEvent handleAddUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note, String detail )
            throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( Calendar.getInstance().getTime() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setEventType( auditEventType );
        auditEvent.setNote( note );
        auditEvent.setDetail( detail );
        return this.getAuditTrailDao().addEvent( auditable, auditEvent );
    }

    @Override
    protected void handleAddComment( Auditable auditable, String comment ) throws Exception {
        AuditEventType type = CommentedEvent.Factory.newInstance();
        this.addUpdateEvent( auditable, type, comment );

    }

    @Override
    protected void handleAddOkFlag( Auditable auditable, String comment, String detail ) throws Exception {
        // TODO possibly don't allow this if there isn't already a trouble event on this object. That is, maybe OK
        // should only be used to reverse "trouble".
        AuditEventType type = OKStatusFlagEvent.Factory.newInstance();
        this.addUpdateEvent( auditable, type, comment, detail );
    }

    @Override
    protected void handleAddTroubleFlag( Auditable auditable, String comment, String detail ) throws Exception {
        AuditEventType type = TroubleStatusFlagEvent.Factory.newInstance();
        this.addUpdateEvent( auditable, type, comment, detail );
    }

    @Override
    protected void handleAddValidatedFlag( Auditable auditable, String comment, String detail ) throws Exception {
        AuditEventType type = ValidatedFlagEvent.Factory.newInstance();
        this.addUpdateEvent( auditable, type, comment, detail );

    }

    @Override
    protected AuditEvent handleGetLastTroubleEvent( Auditable auditable ) throws Exception {
        thaw( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        List<AuditEvent> events = ( List<AuditEvent> ) auditTrail.getEvents();
        AuditEvent lastOK = null;
        for ( int i = events.size() - 1; i >= 0; i-- ) {
            AuditEvent e = events.get( i );
            if ( e.getEventType() instanceof TroubleStatusFlagEvent ) {
                if ( lastOK == null ) {
                    return e;
                }
            } else if ( e.getEventType() instanceof OKStatusFlagEvent ) {
                lastOK = e;
            }
        }
        return null;
    }

    @Override
    protected AuditEvent handleGetLastValidationEvent( Auditable auditable ) throws Exception {
        thaw( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        List<AuditEvent> events = ( List<AuditEvent> ) auditTrail.getEvents();
        for ( int i = events.size() - 1; i >= 0; i-- ) {
            AuditEvent e = events.get( i );
            if ( e.getEventType() instanceof ValidatedFlagEvent ) {
                return e;
            }
        }
        return null;
    }
}