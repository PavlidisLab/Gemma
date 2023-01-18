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
package ubic.gemma.rest.util;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.apachecommons.CommonsLog;

import javax.servlet.ServletConfig;

/**
 * Utilities for interacting with {@link OpenAPI} at runtime.
 */
@CommonsLog
public class OpenApiUtils {

    /**
     * Obtain the {@link OpenAPI} definition for this RESTful API.
     *
     * TODO: make this injectable via {@link javax.ws.rs.core.Context}.
     */
    public static OpenAPI getOpenApi( ServletConfig servletConfig ) {
        try {
            return new JaxrsOpenApiContextBuilder<>()
                    .servletConfig( servletConfig )
                    // if we don't set that, it will reuse the default context which is already built, but for some very
                    // obscure reason ignores the 'openApi.configuration.location' init parameter. Using a different
                    // context identifier will result in a newly, built-from-scratch context.
                    .ctxId( "ubic.gemma.web.services.rest" )
                    .buildContext( true ).read();
        } catch ( OpenApiConfigurationException e ) {
            throw new RuntimeException( e.getMessage(), e );
        }
    }
}
