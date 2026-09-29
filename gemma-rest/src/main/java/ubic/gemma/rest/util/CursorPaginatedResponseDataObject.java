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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.CursorPage;

import java.util.List;

/**
 * Response wrapper for cursor-paginated results. Parallels
 * {@link PaginatedResponseDataObject} but emits {@code nextCursor} / {@code prevCursor}
 * tokens in place of {@code offset}, and lets {@code totalElements} default to
 * {@code null} (cursor mode does not compute a count per request).
 *
 * @author phase3
 */
@Getter
public class CursorPaginatedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    @Schema(description = "The properties the results are grouped by.")
    private final String[] groupBy;
    @Schema(description = "How the results are ordered. Cursor mode currently forces an ascending `id` sort.")
    private final SortValueObject sort;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The page size that was applied.")
    private final Integer limit;
    @Schema(description = "Token for the page after this one, or null when this is the last page.")
    private final String nextCursor;
    @Schema(description = "Token for the page before this one, or null when this is the first page.")
    private final String prevCursor;
    @Schema(description = "How many results match in total. Null by default in cursor mode, which does not run a count query per request — unlike offset mode, where it is always populated.")
    private final Long totalElements;

    public CursorPaginatedResponseDataObject( CursorPage<T> payload, String[] groupBy ) {
        super( payload );
        this.limit = payload.getLimit();
        this.nextCursor = payload.getNextCursor();
        this.prevCursor = payload.getPrevCursor();
        this.totalElements = payload.getTotalElements();
        this.sort = payload.getSort() != null ? new SortValueObject( payload.getSort() ) : null;
        this.groupBy = groupBy;
    }
}
