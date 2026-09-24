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
import ubic.gemma.rest.util.CurationCommitConflictException;
import ubic.gemma.rest.util.WellComposedError;
import ubic.gemma.rest.util.WellComposedErrorBody;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.concurrent.Future;

/**
 * Maps a {@link CurationCommitConflictException} to a 409 whose {@code errors[0].reason} is the stable code for
 * which conflict it was, so a client routes on the code rather than on the wording of the message.
 * <p>
 * More specific than {@code WebApplicationExceptionMapper}, which would otherwise answer for it and emit the
 * status alone.
 *
 * @author gemma
 */
@Provider
@Component
public class CurationCommitConflictExceptionMapper extends AbstractExceptionMapper<CurationCommitConflictException> {

    @Autowired
    public CurationCommitConflictExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, @Qualifier("openApi") Future<OpenAPI> spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( CurationCommitConflictException exception ) {
        return Response.Status.CONFLICT;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( CurationCommitConflictException exception ) {
        return WellComposedErrorBody.builder()
                .code( Response.Status.CONFLICT.getStatusCode() )
                .message( exception.getMessage() )
                .errors( Collections.singletonList( WellComposedError.builder()
                        .reason( exception.getReason().name() )
                        .message( exception.getMessage() )
                        .build() ) )
                .build();
    }
}
