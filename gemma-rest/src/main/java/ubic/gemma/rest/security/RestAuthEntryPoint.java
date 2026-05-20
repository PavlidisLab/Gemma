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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.rest.util.BuildInfoValueObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

/**
 * Implementation of {@link AuthenticationEntryPoint} for the RESTful API to handle authentication.
 * <p>
 * Promoted to {@code @Component("restAuthEntryPoint")} as part of Phase 1 of
 * {@code GEMMA_REST_STANDALONE_RECCE.md} so the bean is producible without
 * gemma-web's {@code applicationContext-security.xml}. The matching XML bean
 * definition in {@code gemma-web/applicationContext-security.xml} was removed
 * to avoid a bean-id collision; the {@code <s:http pattern="/rest/v2/**">}
 * chain in that XML still resolves {@code entry-point-ref="restAuthEntryPoint"}
 * by name from this component.
 * <p>
 * Referenced by {@link RestSecurityConfig#restSecurityFilterChain} via
 * {@code @Qualifier("restAuthEntryPoint")}.
 */
@Slf4j
@Component("restAuthEntryPoint")
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE_401 = "Provided authentication credentials are invalid.";

    private final ObjectMapper objectMapper;
    private final Future<OpenAPI> openAPI;
    private final BuildInfo buildInfo;

    public RestAuthEntryPoint( ObjectMapper objectMapper, @Qualifier("openApi") Future<OpenAPI> openAPI, BuildInfo buildInfo ) {
        this.objectMapper = objectMapper;
        this.openAPI = openAPI;
        this.buildInfo = buildInfo;
    }

    @Override
    public void commence( final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException ) throws IOException {
        String realm;
        String version;
        if ( openAPI.isDone() ) {
            OpenAPI oa = FutureUtils.get( openAPI );
            if ( oa.getInfo() != null ) {
                realm = oa.getInfo().getTitle();
                version = oa.getInfo().getVersion();
            } else {
                log.error( "The 'info' field in the OpenAPI spec is null, will not include version in the error response." );
                realm = "Gemma RESTful API";
                version = null;
            }
        } else {
            log.warn( "The OpenAPI specification hasn't fully loaded yet, will not include version in the error response." );
            realm = "Gemma RESTful API";
            version = null;
        }
        WellComposedErrorBody errorBody = WellComposedErrorBody.builder()
                .code( Response.Status.UNAUTHORIZED.getStatusCode() )
                .message( MESSAGE_401 )
                .build();
        ResponseErrorObject errorObject = ResponseErrorObject.builder()
                .apiVersion( version )
                .buildInfo( BuildInfoValueObject.from( buildInfo ) )
                .error( errorBody )
                .build();
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
