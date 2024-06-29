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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

import ubic.gemma.core.lang.Nullable;
import java.util.*;

/**
 * Object acting as a payload for the ResponseErrorObject.
 * @author tesarst
 */
@Value
public class WellComposedErrorBody {

    int code;

    String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<WellComposedError> errors = new ArrayList<>();

    /**
     * Adds descriptive values from the throwable object to the instance of WellComposedErrorBody.
     * @param t the throwable to read the description from.
     */
    public void addError( Throwable t, @Nullable String location, @Nullable LocationType locationType ) {
        this.errors.add( new WellComposedError( t.getClass().getName(), t.getMessage(), location, locationType ) );
    }
}
