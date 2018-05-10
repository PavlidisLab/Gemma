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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.AbstractService;

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

    @Autowired
    public AuditTrailServiceImpl( AuditTrailDao auditTrailDao, AuditEventDao auditEventDao,
            CurationDetailsService curationDetailsService) {
        super( auditTrailDao );
        this.auditTrailDao = auditTrailDao;
        this.auditEventDao = auditEventDao;
        this.curationDetailsService = curationDetailsService;
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType,
            final String note ) {
        return this.addUpdateEvent( auditable, auditEventType, note, null );
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
    public AuditEvent addUpdateEvent( final Auditable auditable, final AuditEventType auditEventType, final String note,
            final String detail ) {
        //Create new audit event
        AuditEvent auditEvent = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.UPDATE, note, detail, null, auditEventType );
        auditEvent = this.auditTrailDao.addEvent( auditable, auditEvent );

        //If object is curatable, update curation details
        if ( auditable instanceof Curatable && auditEvent != null && auditEvent.getEventType() != null ) {
            curationDetailsService.update( ( Curatable ) auditable, auditEvent );
        }

        //return the newly created event
        return auditEvent;
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, String note,
            String detail ) {

        AuditEventType auditEventType;

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
    @Transactional
    public AuditEvent addUpdateEvent( final Auditable auditable, final String note ) {
        return this.addUpdateEvent( auditable, null, note );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEvents( Auditable ad ) {
        return this.auditEventDao.getEvents( ad );
    }

}
