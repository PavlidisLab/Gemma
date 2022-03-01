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
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.ReflectionUtil;

/**
 * @author paul
 */
@EqualsAndHashCode(of = { "resultClass", "resultId" })
public class SearchResult<T extends Identifiable> implements Comparable<SearchResult<? extends Identifiable>> {

    private final Class<? extends Identifiable> resultClass;

    private final long resultId;

    private String highlightedText;

    private Double score = 0.0;

    private T resultObject; // can be null, at least initially, if the resultClass and objectId are provided.

    public SearchResult( @NonNull T resultObject ) {
        if ( resultObject.getId() == null ) {
            throw new IllegalArgumentException( "Result object ID cannot be null." );
        }
        this.resultId = resultObject.getId();
        this.resultObject = resultObject; // FIXME: maybe this is a bad idea. Eventually we would only want value objects.
        this.resultClass = ( Class<? extends Identifiable> ) ReflectionUtil.getBaseForImpl( resultObject.getClass() );
    }

    public SearchResult( @NonNull T searchResult, double score ) {
        this( searchResult );
        this.score = score;
    }

    public SearchResult( @NonNull T searchResult, double score, String matchingText ) {
        this( searchResult );
        this.score = score;
        this.highlightedText = matchingText;
    }

    public SearchResult( @NonNull Class<? extends Identifiable> entityClass, @NonNull Long entityId, double score, String matchingText ) {
        this.resultClass = entityClass;
        this.resultId = entityId;
        this.score = score;
        this.highlightedText = matchingText;
    }

    public SearchResult( @NonNull Class<? extends Identifiable> entityClass, @NonNull T entity, double score, String matchingText ) {
        this( entityClass, entity.getId(), score, matchingText );
        this.resultObject = entity;
    }

    @Override
    public int compareTo( SearchResult<?> o ) {
        return -this.score.compareTo( o.getScore() );
    }

    public String getHighlightedText() {
        return highlightedText;
    }

    public void setHighlightedText( String highlightedText ) {
        this.highlightedText = highlightedText;
    }

    /**
     * @return the id for the underlying result entity.
     */
    public long getResultId() {
        return resultId;
    }

    public Class<? extends Identifiable> getResultClass() {
        return resultClass;
    }

    public T getResultObject() {
        return this.resultObject;
    }

    /**
     * Set the result object.
     *
     * @throws IllegalArgumentException if the provided result object IDs differs from {@link #getResultId()}.
     */
    public void setResultObject( @NonNull T resultObject ) {
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

    public Double getScore() {
        return score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    @Override
    public String toString() {
        return resultClass.getSimpleName() + "[ID=" + this.resultId + "] matched in: "
                + ( this.highlightedText != null ? "'" + this.highlightedText + "'" : "(?)" );
    }
}
