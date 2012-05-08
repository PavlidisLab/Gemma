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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

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
    public java.util.Collection<? extends User> create( final java.util.Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.create - 'entities' can not be null" );
        }
        for ( User user : entities ) {
            create( user );
        }
        return entities;
    }

    
    public Collection<? extends User> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from UserImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    public User create( final User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.create - 'user' can not be null" );
        }
        this.getHibernateTemplate().save( user );
        return user;

    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByEmail(int, java.lang.String)
     */
    public User findByEmail( final java.lang.String email ) {
        return this.findByEmail( "from UserImpl c where c.email = :email", email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFirstAndLastName(int, java.lang.String,
     *      java.lang.String)
     */
    public Collection<User> findByFirstAndLastName( final String name,
            final String secondName ) {
        return this
                .findByFirstAndLastName(
                        "from UserImpl as user where user.name = :name and user.secondName = :secondName",
                        name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFirstAndLastName(int, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    
    public Collection<User> findByFirstAndLastName( final String queryString,
            final String name, final String secondName ) {
        List<String> argNames = new java.util.ArrayList<String>();
        List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFullName(int, java.lang.String, java.lang.String)
     */

    
    public Collection<User> findByFullName( final String name, final String secondName ) {
        return this.findByFullName(
                "from PersonImpl p where p.firstName=:firstName and p.lastName=:lastName and p.middleName=:middleName",
                name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByLastName(int, java.lang.String)
     */
    public Collection<User> findByLastName( final String lastName ) {
        return this.findByLastName( "from  UserImpl as user where user.lastName = :lastName", lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(int, java.lang.String)
     */
    public User findByUserName( final String userName ) {
        return this.findByUserName( "from UserImpl u where u.userName=:userName", userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#load(int, java.lang.Long)
     */
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
    public Collection<? extends User> loadAll() {
        final Collection<? extends User> results = this.getHibernateTemplate().loadAll(
                UserImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#remove(java.lang.Long)
     */

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

    public void remove( Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#remove(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void remove( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.remove - 'user' can not be null" );
        }
        this.getHibernateTemplate().delete( user );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.update - 'entities' can not be null" );
        }
        for ( User user : entities) {
            update( user );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
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
        Set results = new LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Contact"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return ( User ) result;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFullName(int, java.lang.String, java.lang.String,
     *      java.lang.String)
     */

    
    private Collection findByFullName( final String queryString, final String name,
            final String secondName ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );

        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByLastName(int, java.lang.String, java.lang.String)
     */

    
    private Collection<User> findByLastName( final String queryString,
            final java.lang.String lastName ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( lastName );
        argNames.add( "lastName" );
        List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(int, java.lang.String, java.lang.String)
     */
    
    private User findByUserName( final String queryString, final String userName ) {
//        List<String> argNames = new ArrayList<String>();
//        List<Object> args = new ArrayList<Object>();
//        args.add( userName );
//        argNames.add( "userName" );

        Criteria searchCriteria = this.getSessionFactory().getCurrentSession().createCriteria( UserImpl.class );
        List results = searchCriteria.add( Restrictions.eq( "userName", userName ) ).list();

//        Set results = new LinkedHashSet( 
//                
//                .getHibernateTemplate().findByNamedParam( queryString,
//                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new InvalidDataAccessResourceUsageException(
                    "More than one instance of 'User"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( User ) result;
    }

}