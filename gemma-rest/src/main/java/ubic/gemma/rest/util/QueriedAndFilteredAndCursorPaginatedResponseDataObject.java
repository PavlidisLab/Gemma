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
 * Cursor-mode counterpart to {@link QueriedAndFilteredAndPaginatedResponseDataObject}.
 * Adds the echoed {@code query} field on top of {@link FilteredAndCursorPaginatedResponseDataObject}
 * so cursor-mode responses for endpoints that accept both {@code ?query=} and {@code ?filter=}
 * args preserve the same shape as their offset-mode counterparts (minus {@code offset}, plus
 * {@code nextCursor}/{@code prevCursor}).
 *
 * @author phase3
 */
@Getter
public class QueriedAndFilteredAndCursorPaginatedResponseDataObject<T> extends FilteredAndCursorPaginatedResponseDataObject<T> {

    private final String query;

    public QueriedAndFilteredAndCursorPaginatedResponseDataObject( CursorPage<T> payload, @Nullable String query, @Nullable Filters filters, @Nullable String[] groupBy ) {
        super( payload, filters, groupBy );
        this.query = query;
    }
}
