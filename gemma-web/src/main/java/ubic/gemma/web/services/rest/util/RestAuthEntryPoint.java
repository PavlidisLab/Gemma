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
package ubic.gemma.web.services.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.context.ServletConfigAware;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

@SecurityScheme(name = "restBasicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class RestAuthEntryPoint extends BasicAuthenticationEntryPoint implements ServletConfigAware {

    private static Log log = LogFactory.getLog( RestAuthEntryPoint.class );

    private static final String MESSAGE_401 = "Provided authentication credentials are invalid.";

    private ServletConfig servletConfig;

    @Override
    public void afterPropertiesSet() {
        setRealmName( "Gemma rest api" );
    }

    @Override
    public void commence( final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException ) {

        response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        // using 'xBasic' instead of 'basic' to prevent default browser login popup
        response.addHeader( "WWW-Authenticate", "xBasic realm=" + getRealmName() + "" );
        response.addHeader( "Access-Control-Allow-Headers", "**Authorization**,authorization" ); // necessary for vue gembrow access
        response.setContentType( "application/json" );

        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.UNAUTHORIZED, MESSAGE_401 );
        ResponseErrorObject errorObject = new ResponseErrorObject( errorBody, OpenApiUtils.getOpenApi( servletConfig ) );
        ObjectMapper mapperObj = new ObjectMapper();

        try {
            String jsonStr = mapperObj.writeValueAsString( errorObject );
            response.getWriter().println( jsonStr );
        } catch ( IOException e ) {
            log.error( e );
        }
    }

    @Override
    public void setServletConfig( ServletConfig servletConfig ) {
        this.servletConfig = servletConfig;
    }
}