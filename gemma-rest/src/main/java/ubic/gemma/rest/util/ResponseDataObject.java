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

/**
 * Wrapper for a non-error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON style-guide</a>
 *
 * @author tesarst
 */
public class ResponseDataObject<T> {

    private final T data;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public ResponseDataObject( T payload ) {
        this.data = payload;
    }

    public T getData() {
        return data;
    }
}
