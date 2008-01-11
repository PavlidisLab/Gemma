/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.security.principal;

import java.util.HashMap;
import java.util.Map;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserDao;

/**
 * Implementation for Acegi
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserDetailsServiceImpl implements UserDetailsService, ApplicationContextAware {

    static UserDao userDao;
    private static ApplicationContext applicationContext;
    private static Map<String, User> userCache = new HashMap<String, User>();

    /**
     * @param userService the userService to set
     */
    @SuppressWarnings("static-access")
    public void setUserDao( UserDao userDao ) {
        this.userDao = userDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    public UserDetails loadUserByUsername( String username ) throws UsernameNotFoundException, DataAccessException {

        User u = getUserForUserName( username );
        userCache.put( username, u );
        return new UserDetailsImpl( u );
    }

    private static User getUserForUserName( String username ) {
        /*
         * This is needed to provide initial authentication to the session, so logging in can be attempted. Alternative:
         * get the user without using an authenticated method. (e.g., through a
         */
        if ( SecurityContextHolder.getContext().getAuthentication() == null ) {
            ProviderManager providerManager = ( ProviderManager ) applicationContext.getBean( "authenticationManager" );
            AuthenticationUtils.anonymousAuthenticate( username, providerManager );
        }

        // FIXME One of the interceptors is prepending the username with UserImpl USERNAME= to give a username of
        // UserImpl USERNAME=administrator. This is why we have this String split.
        String[] strings = StringUtils.split( username, "=" );
        if ( strings.length > 1 ) username = strings[1];

        User u = userDao.findByUserName( username );

        if ( u == null ) {
            throw new UsernameNotFoundException( username + " not found" );
        }

        userCache.put( u.getUserName(), u );

        return u;
    }

    /**
     * Returns a String username (the principal).
     * 
     * @return
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.isAuthenticated();

        if ( auth.getPrincipal() instanceof UserDetails ) {
            return ( ( UserDetails ) auth.getPrincipal() ).getUsername();
        }
        return auth.getPrincipal().toString();

    }

    /**
     * @param userName
     * @return
     */
    public static User getCurrentUser() {
        if ( getCurrentUsername() == null ) {
            throw new IllegalStateException( "No current user!" );
        }
        if ( userCache.containsKey( getCurrentUsername() ) ) {
            return userCache.get( getCurrentUsername() );
        }
        User u = getUserForUserName( getCurrentUsername() );
        userCache.put( getCurrentUsername(), u );
        return u;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @SuppressWarnings("static-access")
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;

    }

}
