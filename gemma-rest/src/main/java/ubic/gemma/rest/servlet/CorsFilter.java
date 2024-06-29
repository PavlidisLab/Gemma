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
package ubic.gemma.rest.servlet;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ubic.gemma.core.lang.Nullable;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter for adding CORS headers to the RESTful API responses.
 * <p>
 * This is mounted on the gemma-rest servlet in the web.xml configuration file.
 */
@CommonsLog
public class CorsFilter extends OncePerRequestFilter {

    private static final String WILDCARD = "*";

    private String allowedOrigins = WILDCARD;
    @Nullable
    private String allowedMethods;
    @Nullable
    private String allowedHeaders;
    private boolean allowCredentials = false;
    private int maxAge = -1;

    @Override
    protected void initFilterBean() {
        log.info( String.format( "CORS is configured to allow requests%s from %s%s%s%s",
                allowCredentials ? " with credentials" : "",
                String.join( ", ", splitAndTrim( allowedOrigins ) ),
                allowedMethods != null ? "\n\tAllowed methods: " + String.join( ", ", splitAndTrim( allowedMethods ) ) : "",
                allowedHeaders != null ? "\n\tAllowed headers: " + String.join( ", ", splitAndTrim( allowedHeaders ) ) : "",
                maxAge >= 0 ? "\n\tMax age: " + maxAge : "" ) );
    }

    @Override
    public void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
            throws IOException, ServletException {
        Assert.isTrue( !allowCredentials || !WILDCARD.equals( allowedOrigins ), "If credentials are allowed, a wildcard cannot be used for allowed origins." );
        Assert.isTrue( !allowCredentials || !WILDCARD.equals( allowedMethods ), "If credentials are allowed, a wildcard cannot be used for allowed methods." );
        Assert.isTrue( !allowCredentials || !WILDCARD.equals( allowedHeaders ), "If credentials are allowed, a wildcard cannot be used for allowed headers." );
        String origin = req.getHeader( "Origin" );
        if ( origin != null ) {
            if ( "*".equals( allowedOrigins ) ) {
                res.addHeader( "Access-Control-Allow-Origin", WILDCARD );
            } else {
                boolean matched = false;
                for ( String allowedOrigin : splitAndTrim( allowedOrigins ) ) {
                    allowedOrigin = allowedOrigin.trim();
                    if ( allowedOrigin.equalsIgnoreCase( origin ) ) {
                        res.addHeader( "Access-Control-Allow-Origin", allowedOrigin );
                        res.addHeader( "Vary", "Origin" );
                        matched = true;
                        break;
                    }
                }
                if ( !matched ) {
                    res.sendError( HttpServletResponse.SC_FORBIDDEN, "Invalid CORS request" );
                    return;
                }
            }
            if ( allowCredentials ) {
                res.addHeader( "Access-Control-Allow-Credentials", "true" );
            }
        }
        if ( isPreflight( req ) ) {
            if ( StringUtils.isNotBlank( allowedMethods ) ) {
                res.addHeader( "Access-Control-Allow-Methods", allowedMethods );
            }
            if ( StringUtils.isNotBlank( allowedHeaders ) ) {
                res.addHeader( "Access-Control-Allow-Headers", allowedHeaders );
            }
            if ( maxAge >= 0 ) {
                res.addIntHeader( "Access-Control-Max-Age", maxAge );
            }
            res.setStatus( HttpStatus.NO_CONTENT.value() );
            return;
        }
        chain.doFilter( req, res );
    }

    private static boolean isPreflight( HttpServletRequest req ) {
        return req.getHeader( "Origin" ) != null && HttpMethod.valueOf( req.getMethod() ).equals( HttpMethod.OPTIONS );
    }

    /**
     * Set the allowed origins of a CORS request.
     * <p>
     * Use a wildcard "*" to allow all.
     */
    public void setAllowedOrigins( String allowedOrigins ) {
        // FIXME: for some reason, Spring does not substitute placeholders in filters and we need that to make the allowed origins configurable.
        WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext( getServletContext() );
        if ( context instanceof ConfigurableApplicationContext ) {
            this.allowedOrigins = ( ( ConfigurableWebApplicationContext ) context ).getBeanFactory()
                    .resolveEmbeddedValue( allowedOrigins );
        } else {
            this.allowedOrigins = allowedOrigins;
            log.warn( "The context does not implement the ConfigurableApplicationContext interface, no placeholder substitution will be performed." );
        }
    }

    /**
     * Set the allowed methods by a CORS request.
     * <p>
     * Use a wildcard "*" to allow all.
     */
    public void setAllowedMethods( @Nullable String allowedMethods ) {
        this.allowedMethods = allowedMethods;
    }

    /**
     * Set the allowed headers by a CORS request.
     * <p>
     * Use a wildcard "*" to allow all.
     */
    public void setAllowedHeaders( @Nullable String allowedHeaders ) {
        this.allowedHeaders = allowedHeaders;
    }

    /**
     * Indicate if the credentials (i.e. cookies, HTTP authentication) can be used by a CORS request.
     * <p>
     * If this is set to true, a wildcard cannot be used for {@link #setAllowedOrigins(String)}, {@link #setAllowedMethods(String)}
     * nor {@link #setAllowedHeaders(String)}.
     */
    public void setAllowCredentials( boolean allowCredentials ) {
        this.allowCredentials = allowCredentials;
    }

    /**
     * Set the maximum time in seconds that the results of a preflight request can be cached by the client.
     */
    public void setMaxAge( int maxAge ) {
        this.maxAge = maxAge;
    }

    private String[] splitAndTrim( String s ) {
        return s.split( "\\s*,\\s*" );
    }
}
