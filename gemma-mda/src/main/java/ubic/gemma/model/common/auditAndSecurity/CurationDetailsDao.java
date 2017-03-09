/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.StatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent;
import ubic.gemma.persistence.AbstractDao;

import java.util.Date;

/**
 * @author tesarst
 */
@Component
public class CurationDetailsDao extends AbstractDao<CurationDetails> {

    @Autowired
    public CurationDetailsDao( SessionFactory sessionFactory ) {
        super( CurationDetailsDao.class );
        super.setSessionFactory( sessionFactory );
    }

    public CurationDetails create( AuditEvent createdEvent ) {
        CurationDetails cd = new CurationDetails( createdEvent.getDate(), createdEvent, false, createdEvent, false,
                createdEvent, null );
        return this.create( cd );
    }

    @Override
    public CurationDetails load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CurationDetailsDao.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( CurationDetails.class, id );
        return ( CurationDetails ) entity;
    }

    public void setTroubled( Auditable auditable, boolean value, StatusFlagEvent troubleEvent ) {
        Hibernate.initialize( auditable );
        auditable.getStatus().setTroubled( value );
        this.update( auditable.getStatus() );
    }

    public void setNeedsAttention( Auditable auditable, boolean value ) {
        Hibernate.initialize( auditable );
        auditable.getStatus().setValidated( value );
        if ( value ) {
            auditable.getStatus().setTroubled( false );
        }
        this.update( auditable.getStatus() );
    }

    public void update( Auditable a, AuditEventType auditEventType ) {

        // note that according to the docs, HibernateProxyHelper is being "phased out".
        a = ( Auditable ) this.getSessionFactory().getCurrentSession()
                .get( HibernateProxyHelper.getClassWithoutInitializingProxy( a ), a.getId() );

        Date now = new Date();
        if ( a.getStatus() == null ) {
            a.setStatus( create() );
        } else {
            this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.NONE ).lock( a.getStatus() );
            this.update( a.getStatus() );
        }

        if ( auditEventType != null ) {
            Class<? extends AuditEventType> eventClass = auditEventType.getClass();
            if ( TroubledStatusFlagEvent.class.isAssignableFrom( eventClass ) ) {
                this.setTroubled( a, true );
            } else if ( NotTroubledFlagEvent.class.isAssignableFrom( eventClass ) ) {
                this.setTroubled( a, false );
            } else if ( NeedsAttentionFlagEvent.class.isAssignableFrom( eventClass ) ) {
                this.setNeedsAttention( a, true );
            } else if ( NeedsAttentionFlagEvent.class.isAssignableFrom( eventClass ) ) {
                this.setNeedsAttention( a, false );
            }
        }

    }

}
