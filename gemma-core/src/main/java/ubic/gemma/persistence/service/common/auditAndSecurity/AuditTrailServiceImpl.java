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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.GenericCuratableDao;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * @author pavlidis
 * @see AuditTrailService
 */
@Service
public class AuditTrailServiceImpl extends AbstractService<AuditTrail> implements AuditTrailService {

    private final AuditTrailDao auditTrailDao;

    private final AuditEventDao auditEventDao;

    private final GenericCuratableDao curatableDao;

    private final UserManager userManager;

    private final SessionFactory sessionFactory;

    @Autowired
    public AuditTrailServiceImpl( AuditTrailDao auditTrailDao, AuditEventDao auditEventDao,
            GenericCuratableDao curatableDao, UserManager userManager, SessionFactory sessionFactory ) {
        super( auditTrailDao );
        this.auditTrailDao = auditTrailDao;
        this.auditEventDao = auditEventDao;
        this.curatableDao = curatableDao;
        this.userManager = userManager;
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( final Auditable auditable, final String note ) {
        return doAddUpdateEvent( auditable, null, note, null, null );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note ) {
        return doAddUpdateEvent( auditable, getAuditEventType( type ), note, null, null );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, String detail ) {
        return doAddUpdateEvent( auditable, getAuditEventType( type ), note, detail, null );
    }

    /**
     * This is using the {@link Propagation#REQUIRES_NEW} so that if the throwable is raised, it will not roll back the
     * audit trail event.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, Throwable throwable ) {
        return doAddUpdateEvent( auditable, getAuditEventType( type ), note, ExceptionUtils.getStackTrace( throwable ), null );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, @Nullable String detail, Date performedDate ) {
        return doAddUpdateEvent( auditable, getAuditEventType( type ), note, detail, performedDate );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEvents( Auditable ad ) {
        return this.auditEventDao.getEvents( ad );
    }

    private AuditEvent doAddUpdateEvent( Auditable auditable, @Nullable AuditEventType auditEventType, @Nullable String note, @Nullable String detail, @Nullable Date performedDate ) {
        if ( auditable.getId() == null ) {
            throw new IllegalArgumentException( "Cannot add an update event on a transient entity." );
        }
        if ( performedDate == null ) {
            performedDate = new Date();
        } else if ( performedDate.after( new Date() ) ) {
            throw new IllegalArgumentException( "Cannot create an audit event for something that has not yet occurred." );
        }
        //Create new audit event
        AuditEvent auditEvent = AuditEvent.Factory.newInstance( performedDate, AuditAction.UPDATE, note, detail, userManager.getCurrentUser(), auditEventType );
        //If object is curatable, update curation details
        if ( auditable instanceof Curatable ) {
            curatableDao.updateCurationDetailsFromAuditEvent( ( Curatable ) auditable, auditEvent );
        }
        return this.addEvent( auditable, auditEvent );
    }

    private AuditEvent addEvent( final Auditable auditable, final AuditEvent auditEvent ) {
        AuditTrail trail = auditable.getAuditTrail();
        trail = ensureInSession( trail );
        // this is necessary otherwise we would have to guess the event from the audit trail
        AuditEvent persistedAuditEvent = auditEventDao.save( auditEvent );
        trail.getEvents().add( auditEvent );
        auditable.setAuditTrail( auditTrailDao.save( trail ) );
        return persistedAuditEvent;
    }

    private AuditEventType getAuditEventType( Class<? extends AuditEventType> type ) {
        ClassMetadata classMetadata = sessionFactory.getClassMetadata( type );
        if ( classMetadata == null ) {
            throw new IllegalArgumentException( String.format( "%s is not mapped by Hibernate.", type.getName() ) );
        }
        return ( AuditEventType ) classMetadata.instantiate( null, ( SessionImplementor ) sessionFactory.getCurrentSession() );
    }
}
