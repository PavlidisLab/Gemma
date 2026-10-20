/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.LocationType;
import ubic.gemma.rest.util.UnknownQueryParameterException;
import ubic.gemma.rest.util.WellComposedError;
import ubic.gemma.rest.util.WellComposedErrorBody;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Maps an {@link UnknownQueryParameterException} to a 400 whose {@code errors[]} carries one entry per rejected
 * parameter, each naming the parameter in {@code location} with {@code locationType} {@link LocationType#QUERY}.
 * <p>
 * More specific than {@link MalformedArgExceptionMapper} (the exception extends {@code MalformedArgException}), so
 * JAX-RS mapper selection picks this one and the parameter names survive into the body.
 *
 * @author gemma
 */
@Provider
@Component
public class UnknownQueryParameterExceptionMapper extends AbstractExceptionMapper<UnknownQueryParameterException> {

    /**
     * Stable machine-readable code so a client can branch on this without parsing the message.
     */
    static final String REASON = "UNKNOWN_QUERY_PARAMETER";

    @Autowired
    public UnknownQueryParameterExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, @Qualifier("openApi") Future<OpenAPI> spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( UnknownQueryParameterException exception ) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( UnknownQueryParameterException exception ) {
        String accepted = exception.getAcceptedParameters().isEmpty()
                ? "This endpoint does not accept any query parameter."
                : "This endpoint accepts: " + String.join( ", ", exception.getAcceptedParameters() ) + ".";
        List<WellComposedError> errors = exception.getUnknownParameters().stream()
                .map( name -> WellComposedError.builder()
                        .reason( REASON )
                        .message( "'" + name + "' is not a query parameter of this endpoint. " + accepted )
                        .location( name )
                        .locationType( LocationType.QUERY )
                        .build() )
                .collect( Collectors.toList() );
        return WellComposedErrorBody.builder()
                .code( Response.Status.BAD_REQUEST.getStatusCode() )
                .message( exception.getMessage() )
                .errors( errors )
                .build();
    }
}
