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

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

/**
 * Represents paginated results with offset and limit.
 *
 * @see ubic.gemma.persistence.service.FilteringVoEnabledService#loadValueObjects(Filters, Sort, int, int)
 * @author poirigui
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PaginatedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    String[] groupBy;
    SortValueObject sort;
    Integer offset;
    Integer limit;
    Long totalElements;

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
