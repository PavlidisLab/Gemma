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
package ubic.gemma.persistence.util;

import org.springframework.lang.Nullable;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A page of results returned by keyset (cursor) pagination. Parallels {@link Slice}.
 * <p>
 * Unlike {@link Slice}, a {@code CursorPage} carries no offset and the
 * {@code totalElements} count is optional (and {@code null} by default — cursor mode
 * avoids the {@code COUNT(*)} cost of computing a total on every request). The page
 * carries {@code nextCursor} / {@code prevCursor} tokens (also nullable) for the
 * client to use as the {@code ?cursor=} parameter on the following request.
 *
 * @author phase3
 */
public class CursorPage<O> extends AbstractList<O> implements List<O> {

    private final List<O> data;
    @Nullable
    private final Sort sort;
    @Nullable
    private final Integer limit;
    @Nullable
    private final String nextCursor;
    @Nullable
    private final String prevCursor;
    @Nullable
    private final Long totalElements;

    public CursorPage( List<O> data,
            @Nullable Sort sort,
            @Nullable Integer limit,
            @Nullable String nextCursor,
            @Nullable String prevCursor,
            @Nullable Long totalElements ) {
        this.data = data;
        this.sort = sort;
        this.limit = limit;
        this.nextCursor = nextCursor;
        this.prevCursor = prevCursor;
        this.totalElements = totalElements;
    }

    @Override
    public O get( int i ) {
        return data.get( i );
    }

    @Override
    public int size() {
        return data.size();
    }

    @Nullable
    public Sort getSort() {
        return sort;
    }

    @Nullable
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return base64url-encoded cursor token for the next page, or {@code null} if this
     *         is the last page.
     */
    @Nullable
    public String getNextCursor() {
        return nextCursor;
    }

    /**
     * @return base64url-encoded cursor token for the previous page, or {@code null} if
     *         this is the first page (or if backward navigation is not supported by
     *         the producing DAO).
     */
    @Nullable
    public String getPrevCursor() {
        return prevCursor;
    }

    /**
     * @return total element count, or {@code null} when not computed. Cursor pagination
     *         intentionally omits this by default to avoid {@code COUNT(*)} per request;
     *         clients that need it can opt in via the endpoint's
     *         {@code ?includeTotal=true} parameter when available.
     */
    @Nullable
    public Long getTotalElements() {
        return totalElements;
    }

    public <S> CursorPage<S> map( Function<? super O, ? extends S> converter ) {
        return new CursorPage<>(
                this.stream().map( converter ).collect( Collectors.toList() ),
                sort, limit, nextCursor, prevCursor, totalElements );
    }
}
