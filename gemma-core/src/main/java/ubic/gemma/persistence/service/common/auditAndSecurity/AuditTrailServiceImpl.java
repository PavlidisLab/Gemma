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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.GenericCuratableDao;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * @author pavlidis
 * @see AuditTrailService
 */
@Service
public class AuditTrailServiceImpl extends AbstractService<AuditTrail> implements AuditTrailService {

    private final AuditTrailDao auditTrailDao;
    private final GenericCuratableDao curatableDao;
    private final UserManager userManager;
    private final SessionFactory sessionFactory;

    @Autowired
    public AuditTrailServiceImpl( AuditTrailDao auditTrailDao, GenericCuratableDao curatableDao, UserManager userManager,
            SessionFactory sessionFactory ) {
        super( auditTrailDao );
        this.auditTrailDao = auditTrailDao;
        this.curatableDao = curatableDao;
        this.userManager = userManager;
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( final Auditable auditable, final String note ) {
        return doAddUpdateEvent( auditable, null, note, null, null, true );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note ) {
        return doAddUpdateEvent( auditable, type, note, null, null, true );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, String detail ) {
        return doAddUpdateEvent( auditable, type, note, detail, null, true );
    }

    /**
     * This is using the {@link Propagation#REQUIRES_NEW} so that if the throwable is raised, it will not roll back the
     * audit trail event.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, Throwable throwable ) {
        // because of the REQUIRES_NEW, the auditable might originate from a different session, so it would be unsafe to
        // modify it
        // also, for this reason we cannot update the curation details because that can cause a deadlock if those have
        // been altered in the suspended transaction
        Class<?> entityClass = Hibernate.getClass( auditable );
        Long id = auditable.getId();
        auditable = ( Auditable ) sessionFactory.getCurrentSession().get( entityClass, id );
        if ( auditable == null ) {
            log.error( String.format( "Failed to retrieve an auditable entity with class %s and ID %d in order to add an audit event with an exception.\n\tEvent Type: %s%s",
                    entityClass.getName(), id, type.getName(), note != null ? "\n\tNote: " + note : "" ), throwable );
            return createAuditEvent( type, note, ExceptionUtils.getStackTrace( throwable ), null );
        }
        return doAddUpdateEvent( auditable, type, note, ExceptionUtils.getStackTrace( throwable ), null, false );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, @Nullable String detail, Date performedDate ) {
        return doAddUpdateEvent( auditable, type, note, detail, performedDate, true );
    }

    private AuditEvent doAddUpdateEvent( Auditable auditable, @Nullable Class<? extends AuditEventType> auditEventType, @Nullable String note, @Nullable String detail, @Nullable Date performedDate, boolean updateCurationDetails ) {
        if ( auditable.getId() == null ) {
            throw new IllegalArgumentException( "Cannot add an update event on a transient entity." );
        }
        AuditTrail trail = ensureInSession( auditable.getAuditTrail() );
        auditable.setAuditTrail( trail );
        AuditEvent auditEvent = createAuditEvent( auditEventType, note, detail, performedDate );
        // If object is curatable, update curation details
        if ( ( auditable instanceof Curatable ) && updateCurationDetails ) {
            curatableDao.updateCurationDetailsFromAuditEvent( ( Curatable ) auditable, auditEvent );
        }
        trail.getEvents().add( auditEvent );
        // event will be created in cascade
        auditTrailDao.update( trail );
        return auditEvent;
    }

    private AuditEvent createAuditEvent( @Nullable Class<? extends AuditEventType> auditEventType, @Nullable String note, @Nullable String detail, @Nullable Date performedDate ) {
        Assert.isTrue( performedDate == null || performedDate.after( new Date() ), "Cannot create an audit event for something that has not yet occurred." );
        if ( performedDate == null ) {
            performedDate = new Date();
        }
        return AuditEvent.Factory.newInstance( performedDate, AuditAction.UPDATE, note, detail, userManager.getCurrentUser(), auditEventType != null ? getAuditEventType( auditEventType ) : null );
    }

    private AuditEventType getAuditEventType( Class<? extends AuditEventType> type ) {
        ClassMetadata classMetadata = sessionFactory.getClassMetadata( type );
        if ( classMetadata == null ) {
            throw new IllegalArgumentException( String.format( "%s is not mapped by Hibernate.", type.getName() ) );
        }
        return ( AuditEventType ) classMetadata.instantiate( null, ( SessionImplementor ) sessionFactory.getCurrentSession() );
    }
}
