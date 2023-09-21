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
import lombok.ToString;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

/**
 * @author paul
 */
@Data
@EqualsAndHashCode(of = { "resultType", "resultId" })
@ToString(of = { "resultType", "resultId", "resultType", "highlights", "score", "source" })
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
     * Create a search result whose result class differ from the object.
     * <p>
     * This can be useful if you wrap a proxy, or don't want to expose the object class publicly. For example, our
     * {@link ubic.gemma.model.association.phenotype.PhenotypeAssociation} use a {@link CharacteristicValueObject}
     * for the result object.
     */
    public static <T extends Identifiable> SearchResult<T> from( Class<? extends Identifiable> resultType, T entity, double score, @Nullable Map<String, String> highlights, Object source ) {
        if ( entity.getId() == null ) {
            throw new IllegalArgumentException( "Entity ID cannot be null." );
        }
        SearchResult<T> sr = new SearchResult<>( resultType, entity.getId(), score, highlights, source );
        sr.setResultObject( entity );
        return sr;
    }

    /**
     * Shorthand for {@link #from(Class, Identifiable, double, String, Object)} if you don't need to set the score and
     * highlighted text.
     */
    public static <T extends Identifiable> SearchResult<T> from( Class<? extends Identifiable> resultType, T entity, double score, Object source ) {
        if ( entity.getId() == null ) {
            throw new IllegalArgumentException( "Entity ID cannot be null." );
        }
        SearchResult<T> sr = new SearchResult<>( resultType, entity.getId(), score, null, source );
        sr.setResultObject( entity );
        return sr;
    }

    /**
     * Create a new provisional search result with a result type and ID.
     */
    public static <T extends Identifiable> SearchResult<T> from( Class<? extends Identifiable> resultType, long entityId, double score, @Nullable Map<String, String> highlights, Object source ) {
        return new SearchResult<>( resultType, entityId, score, highlights, source );
    }

    public static <T extends Identifiable> SearchResult<T> from( Class<? extends Identifiable> resultType, long entityId, double score, Object source ) {
        return new SearchResult<>( resultType, entityId, score, null, source );
    }

    /**
     * Create a search result from an existing one, replacing the result object with the target one.
     * <p>
     * This is useful if you need to convert the result object (i.e. to a VO) while preserving the metadata (score,
     * highlighted text, etc.).
     */
    public static <T extends Identifiable> SearchResult<T> from( SearchResult<?> original, @Nullable T newResultObject ) {
        SearchResult<T> sr = new SearchResult<>( original.resultType, original.resultId, original.score, original.highlights, original.source );
        sr.setResultObject( newResultObject );
        return sr;
    }

    /**
     * Class of the result, immutable.
     */
    private final Class<? extends Identifiable> resultType;

    /**
     * ID of the result, immutable.
     */
    private final long resultId;

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
     * Highlights for this result.
     * <p>
     * Keys are fields of {@link T} and values are substrings that matched.
     */
    @Nullable
    private Map<String, String> highlights;

    /**
     * Score for ranking this result among other results.
     */
    private final double score;

    /**
     * Object representing the source of this result object.
     * <p>
     * This can simply be a {@link String}.
     */
    private final Object source;

    /**
     * Placeholder for provisional search results.
     * <p>
     * This is used when the class and ID is known beforehand, but the result hasn't been retrieve yet from persistent
     * storage.
     */
    private SearchResult( Class<? extends Identifiable> entityClass, long entityId, double score, @Nullable Map<String, String> highlights, Object source ) {
        this.resultType = entityClass;
        this.resultId = entityId;
        this.score = score;
        this.highlights = highlights;
        this.source = source;
    }

    @Override
    public int compareTo( SearchResult<?> o ) {
        return getComparator().compare( this, o );
    }

    /**
     * Obtain the result ID.
     * <p>
     * For consistency with {@link Identifiable#getId()}, thus returns a {@link Long}. It is however backed internally
     * by a native long and cannot ever be null.
     */
    @Nonnull
    public Long getResultId() {
        return resultId;
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
}
