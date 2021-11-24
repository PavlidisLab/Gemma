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

import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.ReflectionUtil;

/**
 * @author paul
 */
public class SearchResult<T> implements Comparable<SearchResult<T>> {

    private Class<T> resultClass;

    private Long objectId;

    private String highlightedText;

    private Double score = 0.0;

    private T resultObject; // can be null, at least initially, if the resultClass and objectId are provided.

    /**
     *
     * @param searchResult
     */
    public SearchResult( T searchResult ) {
        if ( searchResult == null )
            throw new IllegalArgumentException( "Search result cannot be null" );
        this.resultObject = searchResult; // FIXME: maybe this is a bad idea. Eventually we would only want value objects.
        this.resultClass = ( Class<T> ) ReflectionUtil.getBaseForImpl( searchResult.getClass() );
        this.objectId = EntityUtils.getId( resultObject );
    }

    public SearchResult( T searchResult, double score ) {
        this( searchResult );
        this.score = score;
    }

    public SearchResult( T searchResult, double score, String matchingText ) {
        this( searchResult );
        this.score = score;
        this.highlightedText = matchingText;
    }

    public SearchResult( Class<T> entityClass, Long entityId, double score, String matchingText ) {
        this.resultClass = entityClass;
        this.objectId = entityId;
        this.score = score;
        this.highlightedText = matchingText;
    }

    @Override
    public int compareTo( SearchResult<T> o ) {
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
    public Long getResultId() {
        return objectId;
    }

    public Class<T> getResultClass() {
        return resultClass;
    }

    public T getResultObject() {
        return this.resultObject;
    }

    /**
     * @param resultObject if null, the resultObject is reset to null, but the class and id information will not be
     *                     overwritten.
     */
    public void setResultObject( T resultObject ) {
        this.resultObject = resultObject;
        if ( resultObject != null ) {
            this.resultClass = ( Class<T> ) ReflectionUtil.getBaseForImpl( resultObject.getClass() );
            this.objectId = EntityUtils.getId( resultObject );
        }
    }

    public Double getScore() {
        return score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( objectId == null ) ? 0 : objectId.hashCode() );
        result = prime * result + ( ( resultClass == null ) ? 0 : resultClass.getName().hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        final SearchResult<T> other = ( SearchResult<T> ) obj;
        if ( objectId == null ) {
            if ( other.objectId != null )
                return false;
        } else if ( !objectId.equals( other.objectId ) )
            return false;
        if ( resultClass == null ) {
            return other.resultClass == null;
        }
        return resultClass.getName().equals( other.resultClass.getName() );
    }

    @Override
    public String toString() {
        return this.getResultClass().getSimpleName() + "[ID=" + this.getResultId() + "] matched in: "
                + ( this.highlightedText != null ? "'" + this.highlightedText + "'" : "(?)" );
    }
}
