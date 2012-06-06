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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.OKStatusFlagEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEventImpl;

// Currently, AuditTrail manages Status. That means apart from retrieving status, all operations should go through AuditTrailService.
//FIXME: It's not ideal to have this manage Status. We should either make Status part of AuditTrail, ot AuditTrail part of Status, 
// or have something a bit higher up to manage both Status and AuditTrail. There should be a single place with this logic.
/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService
 * @author pavlidis
 * @version $Id$
 */
@Service
public class AuditTrailServiceImpl implements AuditTrailService {

    private static Log log = LogFactory.getLog( AuditTrailServiceImpl.class.getName() );

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private AuditEventDao auditEventDao;

    @Autowired
    private StatusDao statusDao;

    @Autowired
    private SessionFactory sessionFactory;

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
    public List<AuditEvent> getEvents( Auditable ad ) {
        return this.auditEventDao.getEvents( ad );
    }

    /**
     * @see AuditTrailService#addComment(Auditable, String, String)
     */
    @Override
    public void addComment( final Auditable auditable, final String comment, final String detail ) {
        try {
            AuditEventType type = new CommentedEventImpl();
            this.addUpdateEvent( auditable, type, comment, detail );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addComment(Auditable auditable, String comment, String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailService#addOkFlag(Auditable, String, String)
     */
    @Override
    public void addOkFlag( final Auditable auditable, final String comment, final String detail ) {
        try {
            // TODO possibly don't allow this if there isn't already a trouble event on this object. That is, maybe OK
            // should only be used to reverse "trouble".
            AuditEventType type = new OKStatusFlagEventImpl();
            this.addUpdateEvent( auditable, type, comment, detail );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addOkFlag(Auditable auditable, String comment, String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailService#addTroubleFlag(Auditable, String, String)
     */
    @Override
    public void addTroubleFlag( final Auditable auditable, final String comment, final String detail ) {
        try {
            AuditEventType type = new TroubleStatusFlagEventImpl();
            this.addUpdateEvent( auditable, type, comment, detail );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addTroubleFlag(Auditable auditable, String comment, String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailService#addUpdateEvent(Auditable, String)
     */
    @Override
    public AuditEvent addUpdateEvent( final Auditable auditable, final String note ) {
        try {
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            auditEvent.setAction( AuditAction.UPDATE );
            auditEvent.setNote( note );
            this.statusDao.update( auditable, null );
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addUpdateEvent(Auditable auditable, String note)' --> " + th,
                    th );
        }
    }

    /**
     * @see AuditTrailService#addUpdateEvent(Auditable, AuditEventType, String)
     */
    @Override
    public AuditEvent addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType,
            final String note, boolean detachedAuditable ) {
        try {
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            auditEvent.setAction( AuditAction.UPDATE );
            auditEvent.setEventType( auditEventType );
            auditEvent.setNote( note );

            // FIXME: Temporary solution.
            if ( !this.sessionFactory.getCurrentSession().contains( auditable ) ) {
                // Re-attach if it is not already in current session
                this.sessionFactory.getCurrentSession().update( auditable );
            }

            // TODO: Use AuditHelper?
            this.statusDao.update( auditable, auditEventType );
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addUpdateEvent(Auditable auditable, AuditEventType auditEventType, String note)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailService#addUpdateEvent(Auditable, AuditEventType, String)
     */
    @Override
    public AuditEvent addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType, final String note ) {
        try {
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            auditEvent.setAction( AuditAction.UPDATE );
            auditEvent.setEventType( auditEventType );
            auditEvent.setNote( note );
            this.statusDao.update( auditable, auditEventType );
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addUpdateEvent(Auditable auditable, AuditEventType auditEventType, String note)' --> "
                            + th, th );
        }
    }

    /**
     * @see AuditTrailService#addUpdateEvent(Auditable, AuditEventType, String, String)
     */
    @Override
    public AuditEvent addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType,
            final String note, final String detail ) {
        try {
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            auditEvent.setAction( AuditAction.UPDATE );
            auditEvent.setEventType( auditEventType );
            auditEvent.setNote( note );
            auditEvent.setDetail( detail );
            this.statusDao.update( auditable, auditEventType );
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addUpdateEvent(Auditable auditable, AuditEventType auditEventType, String note, String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addValidatedFlag(ubic.gemma.model.common.Auditable,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void addValidatedFlag( final Auditable auditable, final String comment, final String detail ) {
        try {
            AuditEventType type = new ValidatedFlagEventImpl();
            this.addUpdateEvent( auditable, type, comment, detail );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addValidatedFlag(Auditable auditable, String comment, String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    @Override
    public AuditTrail create( final AuditTrail auditTrail ) {
        try {
            return this.auditTrailDao.create( auditTrail );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.create(ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#getLastTroubleEvent(ubic.gemma.model.common.Auditable)
     */
    @Override
    public AuditEvent getLastTroubleEvent( final Auditable auditable ) {
        try {
            AuditEvent troubleEvent = this.auditEventDao.getLastEvent( auditable, TroubleStatusFlagEventImpl.class );
            if ( troubleEvent == null ) {
                return null;
            }
            AuditEvent okEvent = this.auditEventDao.getLastEvent( auditable, OKStatusFlagEventImpl.class );
            if ( okEvent != null && okEvent.getDate().after( troubleEvent.getDate() ) ) {
                return null;
            }
            return troubleEvent;
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.getLastTroubleEvent(Auditable auditable)' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#getLastValidationEvent(Auditable)
     */
    @Override
    public AuditEvent getLastValidationEvent( final Auditable auditable ) {
        try {
            return this.auditEventDao.getLastEvent( auditable, ValidatedFlagEventImpl.class );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.getLastValidationEvent(Auditable auditable)' --> " + th, th );
        }
    }
    
    public List<? extends Auditable> getEntitiesWithEvent( Class<? extends Auditable> entityClass, Class<? extends AuditEventType> auditEventClass ){
        return ( List<? extends Auditable> ) this.auditTrailDao.getEntitiesWithEvent( entityClass, auditEventClass );
    }

}
