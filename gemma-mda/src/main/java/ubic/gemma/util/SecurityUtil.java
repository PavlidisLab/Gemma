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
package ubic.gemma.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserImpl;

/**
 * @author keshav
 * @version $Id$
 */
public class SecurityUtil {
    private static Log log = LogFactory.getLog( SecurityUtil.class );

    public static final String ANONYMOUS = "anonymous";
    public static final String GUEST_USERNAME = "test";

    /**
     * Clears the Authentication object.
     */
    public static void flushAuthentication() {
        /* authentication information no longer needed */
        SecurityContextHolder.getContext().setAuthentication( null );
    }

    /**
     * @param userDetails
     * @return {@link User}
     */
    public static User getUserFromUserDetails( UserDetails userDetails ) {
        User user = new UserImpl();
        user.setName( userDetails.getUsername() );
        user.setPassword( userDetails.getPassword() );

        return user;
    }

    /**
     * Sets the security mode such that all threads spawned from the thread where this method is invoked will get the
     * Authentication object.
     */
    public static void passAuthenticationToChildThreads() {

        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

    }

    /**
     * If Authentication object is empty, checks if user with username exists. If so, populates Authentication object
     * with a user with username.
     * 
     * @param userDetailsService
     * @param username
     */
    public static void populateAuthenticationIfEmpty( UserDetailsService userDetailsService, String username ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
        UserDetails userDetails = null;

        if ( auth != null ) return;

        log.info( "Populating authentication object ... " );
        userDetails = userDetailsService.loadUserByUsername( username );
        user = SecurityUtil.getUserFromUserDetails( userDetails );
        GrantedAuthority[] authorities = userDetails.getAuthorities();
        auth = new UsernamePasswordAuthenticationToken( user, user.getPassword(), authorities );
        SecurityContextHolder.getContext().setAuthentication( auth );

    }
}
