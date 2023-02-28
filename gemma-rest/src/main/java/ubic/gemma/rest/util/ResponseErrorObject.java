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

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Value;

/**
 * Wrapper for an error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON style-guide</a>
 *
 * @author tesarst
 */
@Value
public class ResponseErrorObject {

    WellComposedErrorBody error;
    String apiVersion;

    /**
     * @param payload payload containing the error details.
     */
    public ResponseErrorObject( WellComposedErrorBody payload, OpenAPI openApi ) {
        this.error = payload;
        if ( openApi.getInfo() != null ) {
            this.apiVersion = openApi.getInfo().getVersion();
        } else {
            this.apiVersion = null;
        }
    }
}
