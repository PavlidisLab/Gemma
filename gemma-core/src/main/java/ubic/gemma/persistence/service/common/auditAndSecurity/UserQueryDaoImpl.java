/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.UserQuery;

import java.util.Collection;

/**
 * @author Paul
 */
@Repository
public class UserQueryDaoImpl extends HibernateDaoSupport implements UserQueryDao {

    @Autowired
    public UserQueryDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public UserQuery create( UserQuery userQuery ) {
        this.getHibernateTemplate().save( userQuery );
        return userQuery;

    }

    @Override
    public UserQuery load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "UserQuery.load - 'id' can not be null" );
        }
        return ( UserQuery ) this.getSessionFactory().getCurrentSession().get( UserQuery.class, id );
    }

    @Override
    public Collection<? extends UserQuery> loadAll() {
        //noinspection unchecked
        return ( Collection<? extends UserQuery> ) this.getSessionFactory().getCurrentSession()
                .createCriteria( UserQuery.class );
    }

    @Override
    public void remove( UserQuery userQuery ) {
        if ( userQuery == null ) {
            throw new IllegalArgumentException( "UserQuery.remove - 'userQuery' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().delete( userQuery );
    }

}
