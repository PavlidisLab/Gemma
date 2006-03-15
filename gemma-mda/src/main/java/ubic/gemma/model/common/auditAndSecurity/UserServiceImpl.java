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
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;

import ubic.gemma.util.RandomGUID;
import ubic.gemma.util.StringUtil;
import ubic.gemma.Constants;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserService
 * @author pavlidis
 * @version $Id$
 */
public class UserServiceImpl extends ubic.gemma.model.common.auditAndSecurity.UserServiceBase {
    private final transient Log log = LogFactory.getLog( UserServiceImpl.class );

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#getUser(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.common.auditAndSecurity.User handleGetUser( java.lang.String userName )
            throws java.lang.Exception {
        return this.getUserDao().findByUserName( userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#getUsers(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected java.util.Collection handleGetUsers() throws java.lang.Exception {
        return this.getUserDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#FindById(long)
     */
    @Override
    protected User handleFindById( Long id ) throws java.lang.Exception {
        return ( User ) this.getUserDao().load( id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#saveUser(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected User handleSaveUser( ubic.gemma.model.common.auditAndSecurity.User user ) throws UserExistsException {

        // defensive programming...
        for ( UserRole role : user.getRoles() ) {
            role.setUserName( user.getUserName() );
        }

        try {
            user.setConfirmPassword( user.getPassword() );
            return ( User ) this.getUserDao().create( user );
        } catch ( DataIntegrityViolationException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#removeUser(java.lang.String)
     */
    @Override
    protected void handleRemoveUser( java.lang.String userName ) throws java.lang.Exception {

        this.getUserDao().remove( this.getUserDao().findByUserName( userName ) );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#checkLoginCookie(java.lang.String)
     */
    @Override
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
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#createLoginCookie(java.lang.String)
     */
    @Override
    protected String handleCreateLoginCookie( java.lang.String userName ) throws java.lang.Exception {
        UserSession cookie = UserSession.Factory.newInstance();

        User user = this.getUserDao().findByUserName( userName );
        cookie.setUser( user );
        return saveLoginCookie( cookie );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleRemoveLoginCookies(java.lang.String)
     */
    @Override
    protected void handleRemoveLoginCookies( String userName ) throws Exception {
        User user = this.getUserDao().findByUserName( userName );
        user.setUserSessions( null );
        this.getUserDao().update( user );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleAddRole( User user, String role ) throws Exception {
        if ( role == null ) throw new IllegalArgumentException( "Got passed null role!" );
        if ( user == null ) throw new IllegalArgumentException( "Got passed null user" );

        if ( !role.equals( Constants.ADMIN_ROLE ) && !role.equals( Constants.USER_ROLE ) ) {
            throw new IllegalArgumentException( role + " is not a recognized role" );
        }

        UserRole newRole = UserRole.Factory.newInstance();
        newRole.setName( role );
        newRole.setUserName( user.getUserName() );
        newRole = this.getUserRoleService().saveRole( newRole );
        if ( user.getRoles() == null ) user.setRoles( new HashSet() );
        Collection<UserRole> roles = user.getRoles();
        roles.add( newRole );
        user.setRoles( roles );
        this.getUserDao().update( user );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleFindByUserName(java.lang.String)
     */
    @Override
    protected User handleFindByUserName( String userName ) throws Exception {
        return this.getUserDao().findByUserName( userName );
    }

}
