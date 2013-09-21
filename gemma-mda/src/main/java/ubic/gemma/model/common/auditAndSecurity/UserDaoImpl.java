/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.auditAndSecurity;

import gemma.gsec.AuthorityConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.User</code>.
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.User
 * @version $Id$
 */
@Repository
public class UserDaoImpl extends HibernateDaoSupport implements UserDao {

    @Autowired
    public UserDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public void addAuthority( User user, String roleName ) {
        throw new UnsupportedOperationException( "Use user group-based authority instead." );
    }

    @Override
    public void changePassword( User user, String password ) {
        user.setPassword( password );
        this.getSessionFactory().getCurrentSession().update( user );
    }

    @Override
    public Collection<? extends User> create( final Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.create - 'entities' can not be null" );
        }
        for ( User user : entities ) {
            create( user );
        }
        return entities;
    }

    @Override
    public User create( final User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.create - 'user' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().save( user );
        return user;
    }

    @Override
    public User find( User user ) {
        BusinessKey.checkKey( user );
        return this.findByUserName( user.getUserName() );
    }

    @Override
    public User findByEmail( final String email ) {
        return this.findByEmail( "from UserImpl c where c.email = :email", email );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(java.lang.String)
     */
    @Override
    public User findByUserName( final String userName ) {
        Session session = this.getSessionFactory().getCurrentSession();

        List<User> users = session.createCriteria( UserImpl.class ).setFlushMode( FlushMode.MANUAL )
                .add( Restrictions.eq( "userName", userName ) ).list();

        if ( users.isEmpty() ) {
            return null;
        } else if ( users.size() > 1 ) {
            throw new IllegalStateException( "Multiple users with name=" + userName );
        }
        User u = users.get( 0 );
        session.setReadOnly( u, true ); // TESTING
        return u;
    }

    @Override
    public Collection<? extends User> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from UserImpl where id in (:ids)", "ids", ids );
    }

    @Override
    public User load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( UserImpl.class, id );
        return ( User ) entity;
    }

    @Override
    public Collection<? extends User> loadAll() {
        final Collection<? extends User> results = this.getHibernateTemplate().loadAll( UserImpl.class );
        return results;
    }

    @Override
    public Collection<GroupAuthority> loadGroupAuthorities( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr.authorities from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user",
                user );
    }

    @Override
    public Collection<UserGroup> loadGroups( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user", user );
    }

    @Override
    public void remove( Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.remove - 'id' can not be null" );
        }
        User entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.remove - 'user' can not be null" );
        }

        if ( user.getName().equals( AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME ) ) {
            throw new IllegalArgumentException( "Cannot delete user "
                    + AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME );
        }

        this.getHibernateTemplate().delete( user );
    }

    @Override
    public void update( final Collection<? extends User> entities ) {
        // if ( entities == null ) {
        // throw new IllegalArgumentException( "User.update - 'entities' can not be null" );
        // }
        // for ( User user : entities ) {
        // update( user );
        // }
        throw new UnsupportedOperationException( "Cannot update users in bulk" );
    }

    @Override
    public void update( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.update - 'user' can not be null" );
        }

        UserImpl userToUpdate = this.getHibernateTemplate().load( UserImpl.class, user.getId() );
        if ( AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME.equals( userToUpdate.getName() )
                && !userToUpdate.getName().equals( user.getName() ) ) {
            throw new IllegalArgumentException( "Cannot modify name of user "
                    + AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME );
        }

        // we're done with it.
        this.getSessionFactory().getCurrentSession().evict( userToUpdate );

        this.getHibernateTemplate().update( user );
    }

    /**
     * @param queryString
     * @param email
     * @return
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
            throw new InvalidDataAccessResourceUsageException( "More than one instance of 'Contact"
                    + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return result;
    }

}