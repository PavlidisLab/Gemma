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

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ReflectionUtil;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * @author paul
 */
@EqualsAndHashCode(of = { "resultClass", "resultId" })
public class SearchResult<T extends Identifiable> implements Comparable<SearchResult<? extends Identifiable>> {

    /**
     * Obtain a comparator for this search result.
     * <p>
     * Results are compared by {@link #getScore()} in descending order. Note that any search result can be compared
     * regardless of their result type or result object.
     */
    public static Comparator<SearchResult<?>> getComparator() {
        return Comparator.comparing( SearchResult::getScore, Comparator.reverseOrder() );
    }

    private final Class<? extends Identifiable> resultClass;

    private final long resultId;

    /**
     * Result object this search result is refeering to.
     * <p>
     * This can be null, at least initially if the resultClass and objectId are provided.
     * <p>
     * It may also be replaced at a later time via {@link #setResultObject(Identifiable)} and {@link #clearResultObject()}.
     */
    private T resultObject;

    private String highlightedText;

    private double score = 1.0;

    public SearchResult( @NonNull T resultObject ) {
        if ( resultObject.getId() == null ) {
            throw new IllegalArgumentException( "Result object ID cannot be null." );
        }
        this.resultId = resultObject.getId();
        this.resultObject = resultObject; // FIXME: maybe this is a bad idea. Eventually we would only want value objects.
        this.resultClass = ( Class<? extends Identifiable> ) ReflectionUtil.getBaseForImpl( resultObject.getClass() );
    }

    /**
     * Placeholder for provisional search results.
     * <p>
     * This is used when the class and ID is known beforehand, but the result hasn't been retrieve yet from persistent
     * storage.
     */
    public SearchResult( @NonNull Class<? extends Identifiable> entityClass, @NonNull Long entityId ) {
        this.resultClass = entityClass;
        this.resultId = entityId;
    }

    @Override
    public int compareTo( SearchResult<?> o ) {
        return getComparator().compare( this, o );
    }

    /**
     * Obtain the highlighted text for this result.
     * @return the highlighted text in the result, or null if not set
     */
    @Nullable
    public String getHighlightedText() {
        return highlightedText;
    }

    public void setHighlightedText( String highlightedText ) {
        this.highlightedText = highlightedText;
    }

    /**
     * @return the id for the underlying result entity.
     */
    public Long getResultId() {
        return resultId;
    }

    public Class<? extends Identifiable> getResultClass() {
        return resultClass;
    }

    /**
     * Obtain the result object if available.
     */
    @Nullable
    public T getResultObject() {
        return this.resultObject;
    }

    /**
     * Set the result object.
     *
     * @throws IllegalArgumentException if the provided result object IDs differs from {@link #getResultId()}.
     */
    public void setResultObject( @NonNull T resultObject ) {
        if ( resultObject.getId() == null ) {
            throw new IllegalArgumentException( "The result object ID cannot be null." );
        }
        if ( resultObject.getId() != this.resultId ) {
            throw new IllegalArgumentException( "The result object cannot be replaced with one that has a different ID." );
        }
        this.resultObject = resultObject;
    }

    /**
     * Clear the result object.
     */
    public void clearResultObject() {
        this.resultObject = null;
    }

    public double getScore() {
        return score;
    }

    public void setScore( double score ) {
        this.score = score;
    }

    @Override
    public String toString() {
        return resultClass.getSimpleName() + "[ID=" + this.resultId + "] matched in: "
                + ( this.highlightedText != null ? "'" + this.highlightedText + "'" : "(?)" );
    }
}
