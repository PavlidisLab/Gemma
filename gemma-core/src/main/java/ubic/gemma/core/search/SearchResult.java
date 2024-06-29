/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an individual search result.
 * <p>
 * Search result minimally have a type and ID and may have their result object populated at a later time via {@link #setResultObject(Identifiable)}.
 * <p>
 * Results have a score and possibly number of highlights. Two results are considered equal if they have the same type
 * and ID. You may use a {@link SearchResultSet} to combine results in a sensible way, retaining result objects and
 * highlights when a better result is added.
 * @author paul
 * @author poirigui
 * @see SearchSource
 * @see SearchResultSet
 */
@Data
@RequiredArgsConstructor
@EqualsAndHashCode(of = { "resultType", "resultId" })
public class SearchResult<T extends Identifiable> implements Comparable<SearchResult<?>> {

    private static final Comparator<SearchResult<?>> COMPARATOR = Comparator.comparing( SearchResult::getScore, Comparator.reverseOrder() );

    /**
     * Create a search result from a given identifiable entity.
     * <p>
     * The result can be cleared later with {@link #clearResultObject()}.
     */
    public static <T extends Identifiable> SearchResult<T> from( Class<? extends Identifiable> resultType, T entity, double score, @Nullable Map<String, String> highlights, Object source ) {
        Assert.notNull( entity.getId(), "The entity ID cannot be null." );
        SearchResult<T> sr = new SearchResult<>( resultType, entity.getId(), score, highlights, source );
        sr.setResultObject( entity );
        return sr;
    }

    /**
     * Create a new provisional search result with a result type and ID.
     * <p>
     * The result can be set later with {@link #setResultObject(Identifiable)}.
     */
    public static <T extends Identifiable> SearchResult<T> from( Class<? extends Identifiable> resultType, long entityId, double score, @Nullable Map<String, String> highlights, Object source ) {
        return new SearchResult<>( resultType, entityId, score, highlights, source );
    }

    /**
     * Type of search result, immutable.
     */
    private final Class<? extends Identifiable> resultType;

    /**
     * ID of the result, immutable.
     */
    private final long resultId;

    /**
     * Result object this search result is referring to.
     * <p>
     * This can be null, at least initially if the resultType and resultId are provided.
     * <p>
     * It may also be replaced at a later time via {@link #setResultObject(Identifiable)}.
     */
    @Nullable
    private T resultObject;

    /**
     * Score for ranking this result among other results.
     */
    private final double score;

    /**
     * Highlights for this result.
     * <p>
     * Keys are fields of {@link T} and values are substrings that were matched.
     */
    @Nullable
    private final Map<String, String> highlights;

    /**
     * Object representing the source of this result object.
     * <p>
     * This can simply be a {@link String}.
     */
    private final Object source;

    @Override
    public int compareTo( SearchResult<?> o ) {
        return COMPARATOR.compare( this, o );
    }

    @Override
    public String toString() {
        return String.format( "%s Id=%d Score=%.2f%s Source=%s %s", resultType.getSimpleName(), resultId,
                score,
                highlights != null ? " Highlights=" + highlights.keySet().stream().sorted().collect( Collectors.joining( "," ) ) : "",
                source,
                resultObject != null ? "[Not Filled]" : "[Filled]" );
    }

    /**
     * Obtain the result ID.
     * <p>
     * For consistency with {@link Identifiable#getId()}, thus returns a {@link Long}. It is however backed internally
     * by a native long and cannot ever be null.
     */
    public Long getResultId() {
        return resultId;
    }

    /**
     * Set the result object.
     *
     * @throws IllegalArgumentException if the provided result object is null or if its ID differs from {@link #getResultId()}.
     */
    public void setResultObject( T resultObject ) {
        Assert.notNull( resultObject, "The result object cannot be null, use clearResultObject() to unset it." );
        Assert.notNull( resultObject.getId(), "The result object ID cannot be null." );
        Assert.isTrue( resultObject.getId().equals( this.resultId ), "The result object cannot be replaced with one that has a different ID." );
        this.resultObject = resultObject;
    }

    /**
     * Clear the result object.
     */
    public void clearResultObject() {
        this.resultObject = null;
    }

    /**
     * Create a search result from an existing one, replacing the result object with the target one.
     * <p>
     * The new result object does not have to be of the same type as the original result object. This is useful if you
     * need to convert the result object (i.e. to a VO) while preserving the metadata (score, highlighted text, etc.).
     */
    public <S extends Identifiable> SearchResult<S> withResultObject( @Nullable S resultObject ) {
        SearchResult<S> searchResult = new SearchResult<>( resultType, resultId, score, highlights, source );
        if ( resultObject != null ) {
            searchResult.setResultObject( resultObject );
        }
        return searchResult;
    }

    /**
     * Copy this search result with the given highlights.
     */
    public SearchResult<T> withHighlights( Map<String, String> highlights ) {
        SearchResult<T> searchResult = new SearchResult<>( resultType, resultId, score, highlights, source );
        if ( resultObject != null ) {
            searchResult.setResultObject( resultObject );
        }
        return searchResult;
    }
}
