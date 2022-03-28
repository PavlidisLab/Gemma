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
package ubic.gemma.web.services.rest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletConfigAware;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Implementation of {@link AuthenticationEntryPoint} for the RESTful API to handle authentication.
 * <p>
 * This is used in applicationContext-ws-rest.xml as part of Spring Security HTTP configuration.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint, ServletConfigAware {

    private static final String MESSAGE_401 = "Provided authentication credentials are invalid.";

    private final ObjectMapper objectMapper;

    private ServletConfig servletConfig;

    @Autowired
    public RestAuthEntryPoint( ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence( final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException ) throws IOException {
        OpenAPI openAPI = OpenApiUtils.getOpenApi( servletConfig );
        String realm = openAPI.getInfo().getTitle();
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.UNAUTHORIZED, MESSAGE_401 );
        ResponseErrorObject errorObject = new ResponseErrorObject( errorBody, openAPI );
        response.setContentType( MediaType.APPLICATION_JSON );
        // using 'xBasic' instead of 'basic' to prevent default browser login popup
        response.addHeader( "WWW-Authenticate", "xBasic realm=" + realm );
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, objectMapper.writeValueAsString( errorObject ) );
    }

    @Override
    public void setServletConfig( ServletConfig servletConfig ) {
        this.servletConfig = servletConfig;
    }
}
