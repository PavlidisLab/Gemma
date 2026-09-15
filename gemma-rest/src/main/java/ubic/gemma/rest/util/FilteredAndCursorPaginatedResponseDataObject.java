/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;

import org.springframework.lang.Nullable;

/**
 * Cursor-mode counterpart to {@link FilteredAndPaginatedResponseDataObject}. Adds the
 * echoed {@code filter} field on top of {@link CursorPaginatedResponseDataObject} so
 * cursor-mode responses for endpoints that accept a {@code ?filter=} arg preserve
 * the same shape as their offset-mode counterparts (minus {@code offset}, plus
 * {@code nextCursor}/{@code prevCursor}).
 *
 * @author phase3
 */
@Getter
public class FilteredAndCursorPaginatedResponseDataObject<T> extends CursorPaginatedResponseDataObject<T> {

    private final String filter;

    public FilteredAndCursorPaginatedResponseDataObject( CursorPage<T> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
        super( payload, groupBy );
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}
