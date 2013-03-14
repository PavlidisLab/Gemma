/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.util.*;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.User</code>.
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.User
 * @version $Id$
 */
public abstract class UserDaoBase extends HibernateDaoSupport implements UserDao {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends User> create( final java.util.Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.create - 'entities' can not be null" );
        }
        for ( User user : entities ) {
            create( user );
        }
        return entities;
    }

    @Override
    public Collection<? extends User> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from UserImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public User create( final User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.create - 'user' can not be null" );
        }
        this.getHibernateTemplate().save( user );
        this.getSessionFactory().getCurrentSession().flush();
        return user;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByEmail(int, java.lang.String)
     */
    @Override
    public User findByEmail( final java.lang.String email ) {
        return this.findByEmail( "from UserImpl c where c.email = :email", email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#load(int, java.lang.Long)
     */
    @Override
    public User load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( UserImpl.class, id );
        return ( User ) entity;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#loadAll(int)
     */
    @Override
    public Collection<? extends User> loadAll() {
        final Collection<? extends User> results = this.getHibernateTemplate().loadAll( UserImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.remove - 'id' can not be null" );
        }
        User entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#remove(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public void remove( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.remove - 'user' can not be null" );
        }
        this.getHibernateTemplate().delete( user );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.update - 'entities' can not be null" );
        }
        for ( User user : entities ) {
            update( user );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public void update( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.update - 'user' can not be null" );
        }
        this.getHibernateTemplate().update( user );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByEmail(int, java.lang.String, java.lang.String)
     */

    private User findByEmail( final String queryString, final String email ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( email );
        argNames.add( "email" );
        Set<User> results = new LinkedHashSet<User>( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        User result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Contact" + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return result;
    }

}