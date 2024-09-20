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
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Wrapper for a non-error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?#data">Google JSON style-guide</a>
 *
 * @author tesarst
 */
@Getter
public class ResponseDataObject<T> {

    private final T data;

    /**
     * A list of warnings applicable to the request.
     * <p>
     * This is an extension to the Google JSON style-guide.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<WellComposedWarning> warnings = new ArrayList<>();

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public ResponseDataObject( T payload ) {
        this.data = payload;
    }

    /**
     * Add a bunch of warnings to {@link #getWarnings()}.
     */
    public <S extends ResponseDataObject<T>> S addWarnings( Iterable<Throwable> throwables, String location, LocationType locationType ) {
        LinkedHashSet<WellComposedWarning> distinctWarnings = new LinkedHashSet<>();
        for ( Throwable throwable : throwables ) {
            distinctWarnings.add( new WellComposedWarning( throwable.getClass().getName(), throwable.getMessage(), location, locationType ) );
        }
        warnings.addAll( distinctWarnings );
        //noinspection unchecked
        return ( S ) this;
    }
}
