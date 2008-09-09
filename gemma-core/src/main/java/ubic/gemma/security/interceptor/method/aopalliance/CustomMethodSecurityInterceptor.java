/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.security.interceptor.method.aopalliance;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.intercept.AbstractSecurityInterceptor;
import org.acegisecurity.intercept.InterceptorStatusToken;
import org.acegisecurity.intercept.ObjectDefinitionSource;
import org.acegisecurity.intercept.method.MethodDefinitionSource;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.util.SecurityUtil;

/**
 * A custom MethodInterceptor.
 * <p>
 * Provides security interception of AOP Alliance based method invocations.
 * </p>
 * <p>
 * The <code>ObjectDefinitionSource</code> required by this security interceptor is of type {@link
 * MethodDefinitionSource}. This is shared with the AspectJ based security interceptor (<code>AspectJSecurityInterceptor</code>),
 * since both work with Java <code>Method</code>s.
 * </p>
 * <p>
 * Refer to {@link AbstractSecurityInterceptor} for details on the workflow.
 * </p>
 * 
 * @author keshav
 * @author Ben Alex
 * @version $Id$
 */
public class CustomMethodSecurityInterceptor extends AbstractSecurityInterceptor implements MethodInterceptor {

    private static final String ADMINISTRATOR = "administrator";

    private static final String DEFAULT_QUARTZ_SCHEDULER = "DefaultQuartzScheduler";

    private UserDetailsService userDetailsService = null;

    // ~ Instance fields
    // ================================================================================================

    private MethodDefinitionSource objectDefinitionSource;

    // ~ Methods
    // ========================================================================================================

    public MethodDefinitionSource getObjectDefinitionSource() {
        return this.objectDefinitionSource;
    }

    @Override
    public Class getSecureObjectClass() {
        return MethodInvocation.class;
    }

    /**
     * This method should be used to enforce security on a <code>MethodInvocation</code>.
     * 
     * @param mi The method being invoked which requires a security decision
     * @return The returned value from the method invocation
     * @throws Throwable if any error occurs
     */
    public Object invoke( MethodInvocation mi ) throws Throwable {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null ) {

            if ( StringUtils.contains( Thread.currentThread().getName(), DEFAULT_QUARTZ_SCHEDULER ) ) {

                UserDetails userDetails = userDetailsService.loadUserByUsername( ADMINISTRATOR );
                User user = SecurityUtil.getUserFromUserDetails( userDetails );

                GrantedAuthority[] authorities = userDetails.getAuthorities();
                authentication = new UsernamePasswordAuthenticationToken( user, user.getPassword(), authorities );
                SecurityContextHolder.getContext().setAuthentication( authentication );
            }
        }

        Object result = null;
        InterceptorStatusToken token = super.beforeInvocation( mi );

        try {
            result = mi.proceed();
        } finally {
            result = super.afterInvocation( token, result );
        }

        return result;
    }

    @Override
    public ObjectDefinitionSource obtainObjectDefinitionSource() {
        return this.objectDefinitionSource;
    }

    public void setObjectDefinitionSource( MethodDefinitionSource newSource ) {
        this.objectDefinitionSource = newSource;
    }

    /**
     * @param userDetailsService
     */
    public void setUserDetailsService( UserDetailsService userDetailsService ) {
        this.userDetailsService = userDetailsService;
    }
}
