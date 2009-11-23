/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

import org.hibernate.Criteria;
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
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#addAuthority(ubic.gemma.model.common.auditAndSecurity.User,
     * java.lang.String)
     */
    public void addAuthority( User user, String roleName ) {
        throw new UnsupportedOperationException( "User group-based authority instead" );

    }

    /*
     * (non-Javadoc)
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
     * @see ubic.gemma.model.common.auditAndSecurity.UserDaoBase#find(ubic.gemma.model.common.auditAndSecurity.user)
     */
    @SuppressWarnings("unchecked")
    public User find( User user ) {
        try {

            BusinessKey.checkKey( user );

            Criteria queryObject = super.getSession().createCriteria( User.class );

            queryObject.add( Restrictions.eq( "userName", user.getUserName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.auditAndSecurity.User.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( User ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );

        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserDao#loadGroupAuthorities(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    @SuppressWarnings("unchecked")
    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {

        return this.getHibernateTemplate().findByNamedParam(
                "select gr.authorities from UserGroupImpl gr inner join gr.groupMembers m where m = :user ",
                "user", u );

    }

    @SuppressWarnings("unchecked")
    public Collection<UserGroup> loadGroups( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user", user );
    }

}