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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEventImpl;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * @author pavlidis
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService
 */
@Service
public class AuditTrailServiceImpl implements AuditTrailService {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog( AuditTrailServiceImpl.class.getName() );

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private AuditEventDao auditEventDao;

    /**
     * @see AuditTrailService#addComment(AbstractAuditable, String, String)
     */
    @Override
    @Transactional
    public void addComment( final AbstractAuditable auditable, final String comment, final String detail ) {
        AuditEventType type = new CommentedEventImpl();
        this.addUpdateEvent( auditable, type, comment, detail );
    }

    /**
     * @see AuditTrailService#addUpdateEvent(AbstractAuditable, String)
     */
    @Override
    @Transactional
    public AuditEvent addUpdateEvent( final AbstractAuditable auditable, final String note ) {
        return this.addUpdateEvent( auditable, null, note );
    }

    /**
     * @see AuditTrailService#addUpdateEvent(AbstractAuditable, AuditEventType, String)
     */
    @Override
    @Transactional
    public AuditEvent addUpdateEvent( final AbstractAuditable auditable, final AuditEventType auditEventType,
            final String note ) {
        return this.addUpdateEvent( auditable, auditEventType, note, null );
    }

    /**
     * @see AuditTrailService#addUpdateEvent(AbstractAuditable, AuditEventType, String, String)
     */
    @Override
    @Transactional
    public AuditEvent addUpdateEvent( final AbstractAuditable auditable, final AuditEventType auditEventType,
            final String note, final String detail ) {
        AuditEvent auditEvent = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.UPDATE, note, detail, null, auditEventType );
        return this.auditTrailDao.addEvent( auditable, auditEvent );
    }

    @Override
    @Transactional
    public AuditEvent addUpdateEvent( AbstractAuditable auditable, Class<? extends AuditEventType> type, String note,
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

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    @Override
    @Transactional
    public AuditTrail create( final AuditTrail auditTrail ) {
        return this.auditTrailDao.create( auditTrail );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auditable> getEntitiesWithEvent( Class<Auditable> entityClass,
            Class<? extends AuditEventType> auditEventClass ) {
        return ( List<Auditable> ) this.auditTrailDao.getEntitiesWithEvent( entityClass, auditEventClass );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEvents( AbstractAuditable ad ) {
        return this.auditEventDao.getEvents( ad );
    }

}
