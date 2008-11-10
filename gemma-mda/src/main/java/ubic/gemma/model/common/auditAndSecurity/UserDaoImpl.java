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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.User
 */
public class UserDaoImpl extends ubic.gemma.model.common.auditAndSecurity.UserDaoBase {

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.UserDaoBase#find(ubic.gemma.model.common.auditAndSecurity.user)
     */
    @Override
    public User find( Contact contact ) {
        try {
            assert contact instanceof User;

            User user = ( User ) contact;

            BusinessKey.checkKey( user );

            Criteria queryObject = super.getSession( false ).createCriteria( User.class );

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
}