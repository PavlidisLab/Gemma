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

package ubic.gemma.model.common.auditAndSecurity;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
@Repository
public class UserQueryDaoImpl extends HibernateDaoSupport implements UserQueryDao {

    @Autowired
    public UserQueryDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserQueryDao#create(ubic.gemma.model.common.auditAndSecurity.UserQuery)
     */
    @Override
    public UserQuery create( UserQuery userQuery ) {
        this.getHibernateTemplate().save( userQuery );
        return userQuery;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserQueryDao#findByUser(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public Collection<UserQuery> findByUser( User user ) {
        final String query = "select q from UserImpl u inner join u.userQueries q where u = :u";
        return this.getHibernateTemplate().findByNamedParam( query, "u", user );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserQueryDao#findMostRecentForUser(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    @Override
    public UserQuery findMostRecentForUser( User user ) {
        Collection<UserQuery> items = findByUser( user );
        if ( items.isEmpty() ) {
            return null;
        } else if ( items.size() == 1 ) {
            return items.iterator().next();
        }
        List<UserQuery> toSort = new ArrayList<UserQuery>( items );

        Collections.sort( toSort, new Comparator<UserQuery>() {
            @Override
            public int compare( UserQuery o1, UserQuery o2 ) {
                return -o1.getLastUsed().compareTo( o2.getLastUsed() );
            }
        } );

        return toSort.get( 0 );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserQueryDao#load(java.lang.Long)
     */
    @Override
    public UserQuery load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "UserQuery.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( ubic.gemma.model.common.auditAndSecurity.UserQueryImpl.class, id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserQueryDao#loadAll()
     */
    @Override
    public Collection<? extends UserQuery> loadAll() {
        return this.getHibernateTemplate().loadAll( ubic.gemma.model.common.auditAndSecurity.UserQueryImpl.class );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserQueryDao#remove(ubic.gemma.model.common.auditAndSecurity.UserQuery)
     */
    @Override
    public void remove( UserQuery userQuery ) {
        if ( userQuery == null ) {
            throw new IllegalArgumentException( "UserQuery.remove - 'userQuery' can not be null" );
        }
        this.getHibernateTemplate().delete( userQuery );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserQueryDao#removeAllForUser(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    @Override
    public void removeAllForUser( User user ) {
        Collection<UserQuery> items = findByUser( user );
        for ( UserQuery userQuery : items ) {
            this.remove( userQuery );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserQueryDao#removeOldForUser(ubic.gemma.model.common.auditAndSecurity
     * .User, java.sql.Date)
     */
    @Override
    public void removeOldForUser( User user, Date staleDate ) {
        Collection<UserQuery> items = findByUser( user );
        for ( UserQuery userQuery : items ) {
            if ( userQuery.getLastUsed().before( staleDate ) ) {
                this.remove( userQuery );
            }
        }

    }

}
