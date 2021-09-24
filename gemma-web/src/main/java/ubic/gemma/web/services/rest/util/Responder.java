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
package ubic.gemma.web.services.rest.util;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.util.Slice;

import javax.ws.rs.NotFoundException;

/**
 * Handles setting of the response status code and composing a proper payload structure.
 *
 * @author tesarst
 */
public class Responder {

    private static final String DEFAULT_ERR_MSG_NULL_OBJECT = "Requested resource was not found in our database.";

    /**
     * Produce a {@link ResponseDataObject} that wraps the given argument.
     *
     * @param toReturn           an object to be wrapped and published to the API
     * @throws NotFoundException if the argument is null, a suitable {@link ResponseErrorObject} will be subsequently
     *                           produced by {@link ubic.gemma.web.services.rest.providers.NotFoundExceptionMapper}
     * @return a {@link ResponseDataObject} containing the argument
     */
    public static <T> ResponseDataObject<T> respond( T toReturn ) throws NotFoundException {
        if ( toReturn == null ) { // object is null.
            throw new NotFoundException( Responder.DEFAULT_ERR_MSG_NULL_OBJECT );
        } else {
            return new ResponseDataObject<>( toReturn );
        }
    }

    /**
     * Produce a {@link PaginatedResponseDataObject} for a given {@link Slice}.
     * @param payload
     * @param <T>
     * @return
     */
    public static <T> PaginatedResponseDataObject<T> paginate( Slice<T> payload ) throws NotFoundException {
        if ( payload == null ) {
            throw new NotFoundException( Responder.DEFAULT_ERR_MSG_NULL_OBJECT );
        } else {
            return new PaginatedResponseDataObject<>( payload );
        }
    }
}
