/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common.auditAndSecurity;

import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;

import edu.columbia.gemma.util.RandomGUID;
import edu.columbia.gemma.util.StringUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @see edu.columbia.gemma.common.auditAndSecurity.UserService
 * @author pavlidis
 * @version $Id$
 */
public class UserServiceImpl extends edu.columbia.gemma.common.auditAndSecurity.UserServiceBase {
    private final transient Log log = LogFactory.getLog( UserServiceImpl.class );

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.UserService#getUser(java.lang.String)
     */
    protected edu.columbia.gemma.common.auditAndSecurity.User handleGetUser( java.lang.String userName )
            throws java.lang.Exception {
        return this.getUserDao().findByUserName( userName );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.UserService#getUsers(edu.columbia.gemma.common.auditAndSecurity.User)
     */
    protected java.util.List handleGetUsers( edu.columbia.gemma.common.auditAndSecurity.User user )
            throws java.lang.Exception {
        return this.getUserDao().findAllUsers();
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.UserService#saveUser(edu.columbia.gemma.common.auditAndSecurity.User)
     */
    protected User handleSaveUser( edu.columbia.gemma.common.auditAndSecurity.User user ) throws UserExistsException {

        try {
            user.setConfirmPassword(user.getPassword());
            return ( User ) this.getUserDao().create( user );
        } catch ( DataIntegrityViolationException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.UserService#removeUser(java.lang.String)
     */
    protected void handleRemoveUser( java.lang.String userName ) throws java.lang.Exception {

        this.getUserDao().remove( this.getUserDao().findByUserName( userName ) );
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.UserService#checkLoginCookie(java.lang.String)
     */
    protected String handleCheckLoginCookie( java.lang.String value ) throws java.lang.Exception {
        value = StringUtil.decodeString( value );

        String[] values = StringUtils.split( value, "|" );

        // in case of empty username in cookie, return null
        if ( values.length == 1 ) {
            return null;
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "looking up cookieId: " + values[1] );
        }

        UserSession cookie = UserSession.Factory.newInstance();
        cookie = this.getUserSessionDao().findUserSession( values[0], values[1] );

        if ( cookie != null ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "cookieId lookup succeeded, generating new cookieId" );
            }

            return saveLoginCookie( cookie );
        }
        log.debug( "cookieId lookup failed, returning null" );
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.auditAndSecurity.UserService#createLoginCookie(java.lang.String)
     */
    protected String handleCreateLoginCookie( java.lang.String userName ) throws java.lang.Exception {
        UserSession cookie = UserSession.Factory.newInstance();

        User user = this.getUserDao().findByUserName( userName );
        cookie.setUser( user );
        return saveLoginCookie( cookie );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.auditAndSecurity.UserServiceBase#handleRemoveLoginCookies(java.lang.String)
     */
    protected void handleRemoveLoginCookies( String userName ) throws Exception {
        User user = this.getUserDao().findByUserName( userName );
        user.setUserSessions( null );
        this.getUserDao().update( user );
    }

    /**
     * FIXME - this should just take the role name, not the role, because we have to make a new instance anyway.
     * 
     * @see edu.columbia.gemma.common.auditAndSecurity.UserServiceBase#handleAddRole(edu.columbia.gemma.common.auditAndSecurity.Role)
     */
    protected void handleAddRole( User user, UserRole role ) throws Exception {
        if ( role == null ) throw new IllegalArgumentException( "Got passed null role!" );
        if ( user == null ) throw new IllegalArgumentException( "Got passed null user" );
        UserRole newRole = UserRole.Factory.newInstance();
        newRole.setName( role.getName() );
        newRole.setUserName( user.getUserName() );
        newRole = this.getUserRoleService().saveRole( newRole );
        if ( user.getRoles() == null ) user.setRoles( new HashSet() );
        user.getRoles().add( newRole );
    }

    /**
     * Convenience method to set a unique cookie id and save to database
     * 
     * @param cookie
     * @return
     */
    private String saveLoginCookie( UserSession cookie ) {
        cookie.setCookie( new RandomGUID().toString() );
        cookie.setCreateDate( new Date() );
        this.getUserSessionDao().create( cookie );

        return StringUtil.encodeString( cookie.getUser().getUserName() + "|" + cookie.getCookie() );
    }

}