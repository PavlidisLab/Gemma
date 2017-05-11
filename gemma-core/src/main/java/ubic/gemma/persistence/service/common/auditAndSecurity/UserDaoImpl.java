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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import gemma.gsec.AuthorityConstants;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.*;

/**
 * DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.User</code>.
 *
 * @see ubic.gemma.model.common.auditAndSecurity.User
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
        return this.handleFindByEmail( email );
    }

    @Override
    public User findByUserName( final String userName ) {
        Session session = this.getSessionFactory().getCurrentSession();

        //noinspection unchecked
        List<User> users = session.createCriteria( User.class ).setFlushMode( FlushMode.MANUAL )
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
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from User where id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public User load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.load - 'id' can not be null" );
        }
        return ( User ) this.getSessionFactory().getCurrentSession().get( User.class, id );
    }

    @Override
    public Collection<? extends User> loadAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( User.class ).list();
    }

    @Override
    public Collection<GroupAuthority> loadGroupAuthorities( User user ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select gr.authorities from UserGroupImpl gr inner join gr.groupMembers m where m = :user " )
                .setParameter( "user", user ).list();

    }

    @Override
    public Collection<UserGroup> loadGroups( User user ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select gr from UserGroupImpl gr inner join gr.groupMembers m where m = :user " )
                .setParameter( "user", user ).list();
    }

    @Override
    public void remove( Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.remove - 'entities' can not be null" );
        }
        for ( User u : entities ) {
            this.remove( u );
        }
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

        if ( user.getName() != null && user.getName().equals( AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME ) ) {
            throw new IllegalArgumentException(
                    "Cannot delete user " + AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME );
        }
        this.getSessionFactory().getCurrentSession().delete( user );
    }

    @Override
    public void update( final Collection<? extends User> entities ) {
        throw new UnsupportedOperationException( "Cannot update users in bulk" );
    }

    @Override
    public void update( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.update - 'user' can not be null" );
        }

        // check the original isn't 'administrator'. See init-acls.sql
        if ( Objects.equals( user.getId(), AuthorityConstants.REQUIRED_ADMINISTRATOR_ID )
                && !AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME.equals( user.getName() ) ) {
            throw new IllegalArgumentException(
                    "Cannot modify name of user ID=" + AuthorityConstants.REQUIRED_ADMINISTRATOR_ID );
        }

        // FIXME for reasons that remain obscure, I cannot get this to work using a regular session.update.
        // May 11th 2015 just spent 2 hours on this with no result or leads - Steven.
        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET PASSWORD=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getPassword() ).executeUpdate();

        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET USER_NAME=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getUserName() ).executeUpdate();

        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET EMAIL=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getEmail() ).executeUpdate();

        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET ENABLED=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getEnabled() ? 1 : 0 ).executeUpdate();

    }

    private User handleFindByEmail( final String email ) {
        //noinspection unchecked
        List<User> list = this.getSessionFactory().getCurrentSession().createQuery(
                "from User c where c.email = :email" )
                .setParameter( "email", email ).list();
        Set<User> results = new HashSet<>(list);
        User result = null;
        if ( results.size() > 1 ) {
            throw new InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Contact" + "' was found when executing query --> '" + "from User c where c.email = :email"
                            + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return result;
    }

}
