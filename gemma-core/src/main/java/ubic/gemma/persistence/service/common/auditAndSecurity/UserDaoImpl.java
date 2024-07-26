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
        return findOneByProperty( "email", email );
    }

    @Override
    public User findByUserName( final String userName ) {
        return findOneByProperty( "userName", userName );
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
        // check the original isn't 'administrator'. See init-acls.sql
        if ( Objects.equals( user.getId(), AuthorityConstants.REQUIRED_ADMINISTRATOR_ID )
                && !AuthorityConstants.REQUIRED_ADMINISTRATOR_USER_NAME.equals( user.getName() ) ) {
            throw new IllegalArgumentException(
                    "Cannot modify name of user ID=" + AuthorityConstants.REQUIRED_ADMINISTRATOR_ID );
        }

        super.update( user );
    }

    @Override
    protected User findByBusinessKey( User user ) {
        BusinessKey.checkKey( user );
        return this.findByUserName( user.getUserName() );
    }
}
