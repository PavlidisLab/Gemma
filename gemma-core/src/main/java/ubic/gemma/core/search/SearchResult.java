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
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author paul
 */
@Data
@ParametersAreNonnullByDefault
@EqualsAndHashCode(of = { "resultClass", "resultId" })
public class SearchResult<T extends Identifiable> implements Comparable<SearchResult<?>> {

    /**
     * Obtain a comparator for this search result.
     * <p>
     * Results are compared by {@link #getScore()} in descending order. Note that any search result can be compared
     * regardless of their result type or result object.
     */
    public static Comparator<SearchResult<?>> getComparator() {
        return Comparator.comparing( SearchResult::getScore, Comparator.reverseOrder() );
    }

    /**
     * Create a search result from an entity and a score.
     */
    public static <T extends Identifiable> SearchResult<T> from( T entity, double score ) {
        SearchResult<T> sr = new SearchResult<>( entity );
        sr.setScore( score );
        return sr;
    }

    /**
     * Create a search result from an entity, a score and some highlighted text.
     */
    public static <T extends Identifiable> SearchResult<T> from( T entity, double score, String highlightedText ) {
        SearchResult<T> sr = new SearchResult<>( entity );
        sr.setScore( score );
        sr.setHighlightedText( highlightedText );
        return sr;
    }

    /**
     * Class of the result, immutable.
     */
    private final Class<? extends Identifiable> resultClass;

    /**
     * ID of the result, immutable.
     */
    private final Long resultId;

    /**
     * Result object this search result is referring to.
     * <p>
     * This can be null, at least initially if the resultClass and objectId are provided.
     * <p>
     * It may also be replaced at a later time via {@link #setResultObject(Identifiable)}.
     */
    @Nullable
    private T resultObject;

    /**
     * Highlighted text for this result.
     * <p>
     * This is provided by Compass to indicate which part of the result was matched by a query.
     */
    @Nullable
    private String highlightedText;

    /**
     * Score for ranking this result among other results.
     */
    private double score = 1.0;

    public SearchResult( T resultObject ) {
        if ( resultObject.getId() == null ) {
            throw new IllegalArgumentException( "THe result object ID cannot be null." );
        }
        this.resultClass = resultObject.getClass();
        this.resultId = resultObject.getId();
        setResultObject( resultObject );
    }

    /**
     * Placeholder for provisional search results.
     * <p>
     * This is used when the class and ID is known beforehand, but the result hasn't been retrieve yet from persistent
     * storage.
     */
    public SearchResult( Class<? extends Identifiable> entityClass, long entityId ) {
        this.resultClass = entityClass;
        this.resultId = entityId;
    }

    @Override
    public int compareTo( SearchResult<?> o ) {
        return getComparator().compare( this, o );
    }

    /**
     * Set the result object.
     *
     * @throws IllegalArgumentException if the provided result object IDs differs from {@link #getResultId()}.
     */
    public void setResultObject( @Nullable T resultObject ) {
        if ( resultObject != null && resultObject.getId() == null ) {
            throw new IllegalArgumentException( "The result object ID cannot be null." );
        }
        if ( resultObject != null && !Objects.equals( resultObject.getId(), this.resultId ) ) {
            throw new IllegalArgumentException( "The result object cannot be replaced with one that has a different ID." );
        }
        this.resultObject = resultObject;
    }

    @Override
    public String toString() {
        return resultClass.getSimpleName() + "[ID=" + this.resultId + "] matched in: "
                + ( this.highlightedText != null ? "'" + this.highlightedText + "'" : "(?)" );
    }
}
