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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.*;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.AbstractService;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    private final CurationDetailsService curationDetailsService;

    private final UserManager userManager;

    @Autowired
    public AuditTrailServiceImpl( AuditTrailDao auditTrailDao, AuditEventDao auditEventDao,
            CurationDetailsService curationDetailsService, UserManager userManager ) {
        super( auditTrailDao );
        this.auditTrailDao = auditTrailDao;
        this.auditEventDao = auditEventDao;
        this.curationDetailsService = curationDetailsService;
        this.userManager = userManager;
    }

    @Override
    @Transactional
    public void addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType, @Nullable final String note ) {
        doAddUpdateEvent( auditable, auditEventType, note, null );
    }

    /**
     * This method creates a new event in the audit trail of the passed Auditable object. If this object also implements
     * the {@link Curatable} interface, and the passed auditEventType is one of the extensions of
     * {@link CurationDetailsEvent} AuditEventType, this method will pass its result to
     * {@link CurationDetailsService#update(Curatable, AuditEvent)}, to update the curatable objects curation details,
     * before returning it.
     *
     * @param auditable      the auditable object to whose audit trail should a new event be added.
     * @param auditEventType the type of the event that should be created.
     * @param note           string displayed as a note for the event
     * @param detail         detailed description of the event.
     * @return the new AuditEvent that was created in the audit trail of the given auditable object.
     * @see AuditTrailService#addUpdateEvent(Auditable, AuditEventType, String, String)
     */
    @Override
    @Transactional
    public void addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType, @Nullable final String note,
            @Nullable final String detail ) {
        doAddUpdateEvent( auditable, auditEventType, note, detail );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public void addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, @Nullable String detail ) {
        ClassMetadata classMetadata = sessionFactory.getClassMetadata( type );
        if ( classMetadata == null ) {
            throw new IllegalArgumentException( String.format( "%s is not mapped by Hibernate.", type.getName() ) );
        }
        AuditEventType auditEventType = ( AuditEventType ) classMetadata.instantiate( null, ( SessionImplementor ) sessionFactory.getCurrentSession() );
        doAddUpdateEvent( auditable, auditEventType, note, detail );
    }

    @Override
    @Transactional
    public void addUpdateEvent( final Auditable auditable, @Nullable final String note ) {
        doAddUpdateEvent( auditable, null, note, null );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEvents( Auditable ad ) {
        return this.auditEventDao.getEvents( ad );
    }

    private void doAddUpdateEvent( Auditable auditable, @Nullable AuditEventType auditEventType, @Nullable String note, @Nullable String detail ) {
        //Create new audit event
        AuditEvent auditEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, note, detail, userManager.getCurrentUser(), auditEventType );
        //If object is curatable, update curation details
        if ( auditable instanceof Curatable && auditEvent.getEventType() != null ) {
            curationDetailsService.update( ( Curatable ) auditable, auditEvent );
        }
        this.addEvent( auditable, auditEvent );
    }

    private void addEvent( final Auditable auditable, final AuditEvent auditEvent ) {
        AuditTrail trail = auditable.getAuditTrail();
        if ( trail == null ) {
            /*
             * Note: this step should be done by the AuditAdvice when the entity was first created, so this is just
             * defensive.
             */
            log.warn( String.format( "AuditTrail is null for %s. It should have been initialized by the AuditAdvice when the entity was first created.",
                    auditable ) );
            trail = AuditTrail.Factory.newInstance();
        }
        trail.addEvent( auditEvent );
        auditable.setAuditTrail( auditTrailDao.save( trail ) );
    }
}
