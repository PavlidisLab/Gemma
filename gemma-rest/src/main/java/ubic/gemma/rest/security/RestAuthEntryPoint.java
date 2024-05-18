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
package ubic.gemma.rest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.BuildInfoValueObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link AuthenticationEntryPoint} for the RESTful API to handle authentication.
 * <p>
 * This is used in applicationContext-ws-rest.xml as part of Spring Security HTTP configuration.
 */
@CommonsLog
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE_401 = "Provided authentication credentials are invalid.";

    private final ObjectMapper objectMapper;
    private final OpenAPI openAPI;
    private final BuildInfo buildInfo;

    public RestAuthEntryPoint( ObjectMapper objectMapper, OpenAPI openAPI, BuildInfo buildInfo ) {
        this.objectMapper = objectMapper;
        this.openAPI = openAPI;
        this.buildInfo = buildInfo;
    }

    @Override
    public void commence( final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException ) throws IOException {
        String realm;
        String version;
        if ( openAPI.getInfo() != null ) {
            realm = openAPI.getInfo().getTitle();
            version = openAPI.getInfo().getVersion();
        } else {
            log.error( "The 'info' field in the OpenAPI spec is null, will not include version in the error response." );
            realm = "Gemma RESTful API";
            version = null;
        }
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.UNAUTHORIZED, MESSAGE_401 );
        ResponseErrorObject errorObject = new ResponseErrorObject( errorBody, version, new BuildInfoValueObject( buildInfo ) );
        response.setContentType( MediaType.APPLICATION_JSON );
        // using 'xBasic' instead of 'basic' to prevent default browser login popup
        response.addHeader( "WWW-Authenticate", "xBasic realm=" + realm );
        response.setStatus( isXmlHttpRequest( request ) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_UNAUTHORIZED );
        response.setCharacterEncoding( StandardCharsets.UTF_8.name() );
        objectMapper.writeValue( response.getWriter(), errorObject );
        response.flushBuffer();
    }

    private boolean isXmlHttpRequest( HttpServletRequest request ) {
        return "XMLHttpRequest".equalsIgnoreCase( request.getHeader( "X-Requested-With" ) );
    }
}
