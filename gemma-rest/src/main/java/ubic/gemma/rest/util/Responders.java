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

import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.ws.rs.NotFoundException;
import java.util.List;

/**
 * Handles setting of the response status code and composing a proper payload structure.
 *
 * @author tesarst
 */
public class Responders {

    private static final String DEFAULT_ERR_MSG_NULL_OBJECT = "Requested resource was not found in our database.";

    /**
     * Produce a {@link ResponseDataObject} that wraps the given argument.
     *
     * @param payload an object to be wrapped and published to the API
     * @return a {@link ResponseDataObject} containing the argument
     * @throws NotFoundException if the argument is null, a suitable {@link ResponseErrorObject} will be subsequently
     *                           produced by {@link ubic.gemma.rest.providers.NotFoundExceptionMapper}
     */
    public static <T> ResponseDataObject<T> respond( @Nullable T payload ) throws NotFoundException {
        if ( payload == null ) { // object is null.
            throw new NotFoundException( Responders.DEFAULT_ERR_MSG_NULL_OBJECT );
        } else {
            return new ResponseDataObject<>( payload );
        }
    }

    /**
     * Produce all results.
     */
    public static <T> FilteredResponseDataObject<T> all( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        return new FilteredResponseDataObject<>( payload, filters, groupBy, sort );
    }

    public static <T> QueriedAndFilteredResponseDataObject<T> all( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        return new QueriedAndFilteredResponseDataObject<>( payload, query, filters, groupBy, sort );
    }

    /**
     * Produce top results.
     */
    public static <T> FilteredAndLimitedResponseDataObject<T> top( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, Integer limit ) {
        return new FilteredAndLimitedResponseDataObject<>( payload, filters, groupBy, sort, limit );
    }

    /**
     * Paginate results.
     */
    public static <T> PaginatedResponseDataObject<T> paginate( Slice<T> payload, String[] groupBy ) {
        return new PaginatedResponseDataObject<>( payload, groupBy );
    }

    /**
     * Paginate results with a filter.
     */
    public static <T> FilteredAndPaginatedResponseDataObject<T> paginate( Slice<T> payload, @Nullable Filters filters, String[] groupBy ) {
        return new FilteredAndPaginatedResponseDataObject<>( payload, filters, groupBy );
    }

    /**
     * Paginate results with a filter using a callback to produce the payload.
     */
    public static <T> FilteredAndPaginatedResponseDataObject<T> paginate( FilterMethod<T> filterMethod, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, int offset, int limit ) {
        return paginate( filterMethod.load( filters, sort, offset, limit ), filters, groupBy );
    }

    /**
     * Paginate results with a filter and a full-text query.
     */
    public static <T> QueriedAndFilteredAndPaginatedResponseDataObject<T> paginate( Slice<T> payload, String query, @Nullable Filters filters, String[] groupBy ) {
        return new QueriedAndFilteredAndPaginatedResponseDataObject<>( payload, query, filters, groupBy );
    }

    /**
     * A functional interface matching the signature of a paginating service method.
     *
     * @see ubic.gemma.persistence.service.FilteringVoEnabledService#loadValueObjects(Filters, Sort, int, int)
     */
    @FunctionalInterface
    public interface FilterMethod<T> {
        Slice<T> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );
    }
}
