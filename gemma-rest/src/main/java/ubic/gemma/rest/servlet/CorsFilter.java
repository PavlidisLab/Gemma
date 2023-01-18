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

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

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
public class CorsFilter extends OncePerRequestFilter {

    @Override
    public void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
            throws IOException, ServletException {
        if ( req.getHeader( "Origin" ) != null ) {
            res.addHeader( "Access-Control-Allow-Origin", "*" );
        }
        if ( isPreflight( req ) ) {
            res.addHeader( "Access-Control-Allow-Headers", "Authorization,Content-Type" );
            res.setStatus( HttpStatus.NO_CONTENT.value() );
            return;
        }
        chain.doFilter( req, res );
    }

    private static boolean isPreflight( HttpServletRequest req ) {
        return req.getHeader( "Origin" ) != null && HttpMethod.valueOf( req.getMethod() ).equals( HttpMethod.OPTIONS );
    }
}
