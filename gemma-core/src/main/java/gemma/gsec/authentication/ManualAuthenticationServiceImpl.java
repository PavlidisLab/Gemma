/*
 * The Gemma project
 *
 * Copyright (c) 2006-2010 University of British Columbia
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
package gemma.gsec.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/**
 * Process authentication requests that come from outside a web context. This is used for command line interfaces, for
 * example.
 *
 * @author keshav
 * @version $Id: ManualAuthenticationServiceImpl.java,v 1.5 2013/09/22 18:50:42 paul Exp $
 */
public class ManualAuthenticationServiceImpl implements ManualAuthenticationService, ApplicationEventPublisherAware {
    private static final Log log = LogFactory.getLog( ManualAuthenticationServiceImpl.class.getName() );

    private static final String ANONYMOUS_AUTHENTICATION_PRINCIPAL = "anonymousUser";

    private final AuthenticationManager authenticationManager;
    private final String anonymousAuthenticationKey;

    private ApplicationEventPublisher eventPublisher;

    public ManualAuthenticationServiceImpl( AuthenticationManager authenticationManager, String anonymousAuthenticationKey ) {
        this.authenticationManager = authenticationManager;
        this.anonymousAuthenticationKey = anonymousAuthenticationKey;
    }

    @Override
    public Authentication authenticate( String username, String password ) throws AuthenticationException {
        if ( username == null ) {
            username = "";
        }

        if ( password == null ) {
            password = "";
        }

        // now ready to log the user in
        Authentication authRequest = new UsernamePasswordAuthenticationToken( username, password );
        return authenticate( authRequest );
    }

    @Override
    public Authentication authenticateAnonymously() throws AuthenticationException {
        log.debug( "No authentication object in context, providing anonymous authentication" );
        List<GrantedAuthority> gas = new ArrayList<>();

        gas.add( new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) );

        /*
         * "anonymousUser" is defined in org.springframework.security.config.http.AuthenticationConfigBuilder (but is
         * also configurable...).
         */
        return authenticate( new AnonymousAuthenticationToken( anonymousAuthenticationKey, ANONYMOUS_AUTHENTICATION_PRINCIPAL, gas ) );
    }

    private Authentication authenticate( Authentication authRequest ) throws AuthenticationException {
        try {
            Authentication authResult = authenticationManager.authenticate( authRequest );
            if ( this.eventPublisher != null ) {
                eventPublisher.publishEvent( new InteractiveAuthenticationSuccessEvent( authResult, this.getClass() ) );
            } else {
                log.fatal( "No context in which to place the authentication object" );
            }
            return authResult;
        } catch ( AuthenticationException e ) {
            log.debug( "Authentication request failed.", e );
            throw e;
        }
    }

    @Override
    public void setApplicationEventPublisher( ApplicationEventPublisher applicationEventPublisher ) {
        this.eventPublisher = applicationEventPublisher;
    }
}
