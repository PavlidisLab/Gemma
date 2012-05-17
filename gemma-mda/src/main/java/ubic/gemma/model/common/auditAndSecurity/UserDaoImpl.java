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

import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.User
 */
@Repository
public class UserDaoImpl extends ubic.gemma.model.common.auditAndSecurity.UserDaoBase {

    @Autowired
    public UserDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#addAuthority(ubic.gemma.model.common.auditAndSecurity.User,
     * java.lang.String)
     */
    public void addAuthority( User user, String roleName ) {
        throw new UnsupportedOperationException( "User group-based authority instead" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserDao#changePassword(ubic.gemma.model.common.auditAndSecurity.User,
     * java.lang.String)
     */
    public void changePassword( User user, String password ) {
        user.setPassword( password );
        this.getHibernateTemplate().update( user );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserDaoBase#find(ubic.gemma.model.common.auditAndSecurity.user)
     */
    public User find( User user ) {

        BusinessKey.checkKey( user );

        Criteria queryObject = super.getSession().createCriteria( User.class );

        queryObject.add( Restrictions.eq( "userName", user.getUserName() ) );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + ubic.gemma.model.common.auditAndSecurity.User.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.get( 0 );
            }
        }
        return ( User ) result;

    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(int, java.lang.String)
     */
    public User findByUserName( final String userName ) {
        List<?> r = this.getSession().createQuery( "from UserImpl u where u.userName=:userName" ).setCacheable( true )
                .setCacheRegion( "usersByUserName" ).setParameter( "userName", userName ).list();
        //
        // List<?> r = this.getHibernateTemplate().findByNamedParam( "from UserImpl u where u.userName=:userName",
        // "userName", userName );
        if ( r.isEmpty() ) {
            return null;
        } else if ( r.size() > 1 ) {
            throw new IllegalStateException( "Multiple users with name=" + userName );
        }
        return ( User ) r.get( 0 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserDao#loadGroupAuthorities(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {

        return this.getHibernateTemplate().findByNamedParam(
                "select gr.authorities from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user", u );

    }

    public Collection<UserGroup> loadGroups( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user", user );
    }

}