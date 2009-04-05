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
package ubic.gemma.security.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationTrustResolver;
import org.springframework.security.AuthenticationTrustResolverImpl;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.MethodBeforeAdvice;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.util.UserConstants;

/**
 * This helps ensure that users who are not administrators do not modify other users.
 * <p>
 * Adapted from Appfuse 1.9
 * 
 * @author Raible
 * @author pavlidis
 * @see UserSecurityPointcut
 * @version $Id$
 */
public class UserSecurityAdvice implements MethodBeforeAdvice {
    public final static String ACCESS_DENIED = "Access Denied: Only administrators are allowed to modify other users.";
    protected final Log log = LogFactory.getLog( UserSecurityAdvice.class );

    public void before( Method method, Object[] args, Object target ) throws Throwable {
        SecurityContext ctx = SecurityContextHolder.getContext();

        if ( ctx.getAuthentication() != null ) {
            Authentication auth = ctx.getAuthentication();
            boolean administrator = false;
            GrantedAuthority[] roles = auth.getAuthorities();
            for ( int i = 0; i < roles.length; i++ ) {
                if ( roles[i].getAuthority().equals( UserConstants.ADMIN_ROLE ) ) {
                    administrator = true;
                    break;
                }
            }

            User user = ( User ) args[0];
            String username = user.getUserName();

            String currentUser = null;
            if ( auth.getPrincipal() instanceof UserDetails ) {
                currentUser = ( ( UserDetails ) auth.getPrincipal() ).getUsername();
            } else {
                currentUser = String.valueOf( auth.getPrincipal() );
            }

            if ( !username.equals( currentUser ) ) {
                AuthenticationTrustResolver resolver = new AuthenticationTrustResolverImpl();
                // allow new users to signup - this is OK b/c Signup doesn't allow setting of roles
                boolean signupUser = resolver.isAnonymous( auth );
                if ( !signupUser ) {
                    if ( log.isDebugEnabled() ) {
                        log.debug( "Verifying that '" + currentUser + "' can modify '" + username + "'" );
                    }
                    if ( !administrator ) {
                        log.warn( "Access Denied: '" + currentUser + "' tried to modify '" + username + "'!" );
                        throw new AccessDeniedException( ACCESS_DENIED );
                    }
                } else {
                    if ( log.isDebugEnabled() ) {
                        log.debug( "Registering new user '" + username + "'" );
                    }
                }
            }

            // fix for http://issues.appfuse.org/browse/APF-96
            // don't allow users with "user" role to upgrade to "admin" role
            else if ( username.equalsIgnoreCase( currentUser ) && !administrator ) {

                // get the list of roles the user is trying add
                Collection<String> userRoles = new HashSet<String>();
                if ( user.getRoles() != null ) {
                    for ( UserRole role : user.getRoles() ) {
                        userRoles.add( role.getName() );
                    }
                }

                // get the list of roles the user currently has
                Collection<String> authorizedRoles = new HashSet<String>();
                for ( int i = 0; i < roles.length; i++ ) {
                    authorizedRoles.add( roles[i].getAuthority() );
                }

                // if they don't match - access denied
                // users aren't allowed to change their roles
                if ( !CollectionUtils.isEqualCollection( userRoles, authorizedRoles ) ) {
                    log.warn( "Access Denied: '" + currentUser + "' tried to change their role(s)!" );
                    throw new AccessDeniedException( ACCESS_DENIED );
                }
            }
        }
    }
}
