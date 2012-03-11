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

import java.util.Date;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.persistence.AbstractDao;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class StatusDaoImpl extends AbstractDao<Status> implements StatusDao {

    @Autowired
    public StatusDaoImpl( SessionFactory sessionFactory ) {
        super( StatusDaoImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public void update( Auditable a ) {
        Date now = new Date();
        if ( a.getStatus() == null ) {
            a.setStatus( create() );
        } else {

            a.getStatus().setLastUpdateDate( now );
            this.update( a.getStatus() );
        }

    }

    @Override
    public void setTroubled( Auditable a, boolean value ) {
        a.getStatus().setTroubled( true );
        if ( value ) {
            a.getStatus().setValidated( false );
        }
        this.update( a.getStatus() );
    }

    @Override
    public void setValidated( Auditable a, boolean value ) {
        a.getStatus().setValidated( true );
        this.update( a.getStatus() );
    }

    @Override
    public Status create() {
        Status s = Status.Factory.newInstance();
        Date now = new Date();
        s.setCreateDate( now );
        s.setLastUpdateDate( now );
        s.setTroubled( false );
        s.setValidated( false );
        return this.create( s );
    }

    @Override
    public void initializeStatus( Auditable d ) {
        Status s = create();
        d.setStatus( s );
        this.getHibernateTemplate().update( d );

    }

    @Override
    public Status load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "StatusDaoImpl.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( StatusImpl.class, id );
        return ( Status ) entity;
    }

}
