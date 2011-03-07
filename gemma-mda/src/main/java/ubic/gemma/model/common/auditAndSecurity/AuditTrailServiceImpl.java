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

import java.lang.reflect.Method;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.OKStatusFlagEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEventImpl;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService
 * @author pavlidis
 * @version $Id$
 */
@Service
public class AuditTrailServiceImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase {

    private static Log log = LogFactory.getLog( AuditTrailServiceImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addUpdateEvent(ubic.gemma.model.common.Auditable,
     * java.lang.Class, java.lang.String, java.lang.String)
     */
    @Override
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, String note,
            String detail ) {

        AuditEventType auditEventType = null;

        try {
            Class<?> factory = Class.forName( type.getName() + "$Factory" );
            Method method = factory.getMethod( "newInstance" );
            auditEventType = ( AuditEventType ) method.invoke( type );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        return this.addUpdateEvent( auditable, auditEventType, note, detail );
    }

    @Override
    protected void handleAddComment( Auditable auditable, String comment, String detail ) throws Exception {
        AuditEventType type = new CommentedEventImpl();
        this.addUpdateEvent( auditable, type, comment, detail );

    }

    @Override
    protected void handleAddOkFlag( Auditable auditable, String comment, String detail ) throws Exception {
        // TODO possibly don't allow this if there isn't already a trouble event on this object. That is, maybe OK
        // should only be used to reverse "trouble".
        AuditEventType type = new OKStatusFlagEventImpl();
        this.addUpdateEvent( auditable, type, comment, detail );
    }

    @Override
    protected void handleAddTroubleFlag( Auditable auditable, String comment, String detail ) throws Exception {
        AuditEventType type = new TroubleStatusFlagEventImpl();
        this.addUpdateEvent( auditable, type, comment, detail );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase#handleAddUpdateEvent(ubic.gemma.model.common.Auditable
     * , ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType, java.lang.String)
     */
    @Override
    protected AuditEvent handleAddUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note )
            throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( new Date() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setEventType( auditEventType );
        auditEvent.setNote( note );
        return this.getAuditTrailDao().addEvent( auditable, auditEvent );
    }

    @Override
    protected AuditEvent handleAddUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note,
            String detail ) throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( new Date() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setEventType( auditEventType );
        auditEvent.setNote( note );
        auditEvent.setDetail( detail );
        return this.getAuditTrailDao().addEvent( auditable, auditEvent );
    }

    @Override
    protected AuditEvent handleAddUpdateEvent( Auditable auditable, String note ) throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( new Date() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setNote( note );
        return this.getAuditTrailDao().addEvent( auditable, auditEvent );
    }

    @Override
    protected void handleAddValidatedFlag( Auditable auditable, String comment, String detail ) throws Exception {
        AuditEventType type = new ValidatedFlagEventImpl();
        this.addUpdateEvent( auditable, type, comment, detail );

    }

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

    @Override
    protected AuditEvent handleGetLastTroubleEvent( Auditable auditable ) throws Exception {
        AuditEvent troubleEvent = getAuditEventDao().getLastEvent( auditable, TroubleStatusFlagEventImpl.class );
        if ( troubleEvent == null ) {
            return null;
        }
        AuditEvent okEvent = getAuditEventDao().getLastEvent( auditable, OKStatusFlagEventImpl.class );
        if ( okEvent != null && okEvent.getDate().after( troubleEvent.getDate() ) ) {
            return null;
        }
        return troubleEvent;

    }

    @Override
    protected AuditEvent handleGetLastValidationEvent( Auditable auditable ) throws Exception {
        return getAuditEventDao().getLastEvent( auditable, ValidatedFlagEventImpl.class );
    }

    @Override
    protected void handleThaw( Auditable auditable ) throws Exception {
        this.getAuditTrailDao().thaw( auditable );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase#handleThaw(ubic.gemma.model.common.auditAndSecurity
     * .AuditTrail)
     */
    @Override
    protected void handleThaw( AuditTrail auditTrail ) throws Exception {
        this.getAuditTrailDao().thaw( auditTrail );

    }
}