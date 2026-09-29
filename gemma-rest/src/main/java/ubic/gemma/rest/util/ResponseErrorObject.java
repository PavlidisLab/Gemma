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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Wrapper for an error response payload compliant with the <a href="https://google.github.io/styleguide/jsoncstyleguide.xml#error">Google JSON style-guide</a>.
 *
 * @author tesarst
 */
@Value
@Builder
@Jacksonized
public class ResponseErrorObject {

    /**
     * API version.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Version of the API that produced the error.")
    String apiVersion;

    /**
     * Build information.
     * <p>
     * This is an extension to the Google JSON style-guide.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Build the error came from. This is an extension to the Google JSON style guide.")
    BuildInfoValueObject buildInfo;

    /**
     * Error beign reported.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The error itself.")
    WellComposedErrorBody error;
}
