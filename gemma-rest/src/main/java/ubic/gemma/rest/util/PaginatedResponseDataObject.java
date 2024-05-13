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

import lombok.Getter;
import ubic.gemma.persistence.util.Slice;

import java.util.List;

/**
 * Represents paginated results with offset and limit.
 *
 * @author poirigui
 * @see Responders#paginate
 */
@Getter
public class PaginatedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    private final String[] groupBy;
    private final SortValueObject sort;
    private final Integer offset;
    private final Integer limit;
    private final Long totalElements;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public PaginatedResponseDataObject( Slice<T> payload, String[] groupBy ) {
        super( payload );
        this.offset = payload.getOffset();
        this.limit = payload.getLimit();
        this.totalElements = payload.getTotalElements();
        this.sort = payload.getSort() != null ? new SortValueObject( payload.getSort() ) : null;
        this.groupBy = groupBy;
    }
}
