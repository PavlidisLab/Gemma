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
package ubic.gemma.web.services.rest.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter for adding CORS headers to the RESTful API responses.
 * <p>
 * This is mounted on the gemma-rest servlet in the web.xml configuration file.
 */
public class CorsFilter implements Filter {

    public CorsFilter() {

    }

    @Override
    public void init( FilterConfig config ) {
    }

    @Override
    public void doFilter( ServletRequest req, ServletResponse res, FilterChain chain )
            throws IOException, ServletException {
        final HttpServletResponse response = ( HttpServletResponse ) res;
        response.addHeader( "Access-Control-Allow-Origin", "*" );
        response.addHeader( "Access-Control-Allow-Headers", "Authorization,Content-Type" );
        chain.doFilter( req, res );
    }

    @Override
    public void destroy() {
    }
}
