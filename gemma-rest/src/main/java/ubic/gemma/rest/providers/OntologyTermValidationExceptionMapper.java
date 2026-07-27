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
import ubic.gemma.rest.util.OntologyTermValidationException;
import ubic.gemma.rest.util.WellComposedError;
import ubic.gemma.rest.util.WellComposedErrorBody;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Maps an {@link OntologyTermValidationException} to a 400 whose {@code errors[]} carries one entry per
 * failing term slot — each with a stable {@code reason} code, a human message naming the resolved label, and
 * a {@code location} (request-body path + clientRef) — so a client can self-correct without guessing.
 *
 * @author gemma
 */
@Provider
@Component
public class OntologyTermValidationExceptionMapper extends AbstractExceptionMapper<OntologyTermValidationException> {

    @Autowired
    public OntologyTermValidationExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, @Qualifier("openApi") Future<OpenAPI> spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( OntologyTermValidationException exception ) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( OntologyTermValidationException exception ) {
        List<WellComposedError> errors = exception.getViolations().stream()
                .map( located -> WellComposedError.builder()
                        .reason( located.getViolation().getReason().name() )
                        .message( located.getViolation().toString() )
                        .location( located.getLocation() )
                        .locationType( LocationType.BODY )
                        .build() )
                .collect( Collectors.toList() );
        return WellComposedErrorBody.builder()
                .code( Response.Status.BAD_REQUEST.getStatusCode() )
                .message( exception.getMessage() )
                .errors( errors )
                .build();
    }
}
