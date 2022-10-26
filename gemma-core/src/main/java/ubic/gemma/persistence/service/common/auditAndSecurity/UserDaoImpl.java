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
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.User</code>.
 *
 * @see ubic.gemma.model.common.auditAndSecurity.User
 */
@Repository
public class UserDaoImpl extends AbstractDao<User> implements UserDao {

    @Autowired
    public UserDaoImpl( SessionFactory sessionFactory ) {
        super( User.class, sessionFactory );
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
    public Collection<GroupAuthority> loadGroupAuthorities( User user ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select gr.authorities from UserGroup gr inner join gr.groupMembers m where m = :user " )
                .setParameter( "user", user ).list();

    }

    @Override
    public Collection<UserGroup> loadGroups( User user ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select gr from UserGroup gr inner join gr.groupMembers m where m = :user " )
                .setParameter( "user", user ).list();
    }

    @Override
    public void remove( User user ) {
        if ( user.getName() != null && user.getName().equals( AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME ) ) {
            throw new IllegalArgumentException(
                    "Cannot remove user " + AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME );
        }
        super.remove( user );
    }

    @Override
    public void update( final Collection<User> entities ) {
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
        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET PASSWORD=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getPassword() ).executeUpdate();

        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET USER_NAME=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getUserName() ).executeUpdate();

        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET EMAIL=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getEmail() ).executeUpdate();

        this.getSessionFactory().getCurrentSession().createSQLQuery( "UPDATE CONTACT SET ENABLED=:a WHERE ID=:id" )
                .setParameter( "id", user.getId() ).setParameter( "a", user.getEnabled() ? 1 : 0 ).executeUpdate();

    }

    @Override
    public User find( User user ) {
        BusinessKey.checkKey( user );
        return this.findByUserName( user.getUserName() );
    }

    private User handleFindByEmail( final String email ) {
        //noinspection unchecked
        return ( User ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from User c where c.email = :email" ).setParameter( "email", email )
                .uniqueResult();
    }

}
