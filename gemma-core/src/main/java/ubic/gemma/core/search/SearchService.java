/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static ubic.gemma.model.common.search.SearchSettings.SearchMode;

/**
 * @author paul
 */
public interface SearchService {

    interface SearchResultMap {

        List<SearchResult<?>> getByResultType( Class<? extends Identifiable> searchResultType );

        /**
         * Obtain results where the result object is of a given type, regardless of the result type.
         */
        <T extends Identifiable> List<SearchResult<T>> getByResultObjectType( Class<T> clazz );

        boolean isEmpty();

        Set<Class<? extends Identifiable>> getResultTypes();

        List<SearchResult<?>> toList();
    }

    /**
     * Obtain a list of fields that can be searched on for the given result type and search mode.
     * <p>
     * The search mode can affect which fields will be looked-up by the search sources. To get all possible fields, use
     * the {@link SearchMode#ACCURATE} mode.
     */
    Set<String> getFields( Class<? extends Identifiable> resultType, SearchMode searchMode );

    /**
     * The results are sorted in order of decreasing score, organized by class. The following objects can be searched
     * for, depending on the configuration of the input object.
     * <ul>
     * <li>Genes
     * <li>ExpressionExperiments
     * <li>CompositeSequences (probes)
     * <li>ArrayDesigns (platforms)
     * <li>Characteristics (e.g., Ontology annotations)
     * <li>BioSequences
     * <li>BibliographicReferences (articles)
     * </ul>
     *
     * @param settings settings
     * @return Map of Class to SearchResults. The results are already filtered for security considerations.
     */
    SearchResultMap search( SearchSettings settings, SearchContext context ) throws SearchException;

    SearchResultMap search( SearchSettings settings ) throws SearchException;

    /**
     * Returns a set of supported result types.
     * <p>
     * This is mainly used to perform a search for everything via {@link SearchSettings#getResultTypes()}.
     */
    Set<Class<? extends Identifiable>> getSupportedResultTypes();

    /**
     * Convert a {@link SearchResult} to its VO flavour.
     * <p>
     * The resulting search result preserve the result ID, score and highlighted text, and {@link SearchResult#getResultType()},
     * but sees its {@link SearchResult#getResultObject()} transformed.
     * <p>
     * The conversion logic is mainly defined by the corresponding {@link ubic.gemma.persistence.service.BaseVoEnabledService}
     * that match the result type. See {@link #getSupportedResultTypes()} for a set of supported result types this o
     * function can handle.
     *
     * @throws IllegalArgumentException if the passed search result is not supported for VO conversion
     */
    <T extends Identifiable, U extends IdentifiableValueObject<T>> SearchResult<U> loadValueObject( SearchResult<T> searchResult ) throws IllegalArgumentException;

    /**
     * Convert a collection of {@link SearchResult} to their VO flavours.
     * <p>
     * Note that since the results might contain a mixture of different result types, the implementation can take
     * advantage of grouping result by types in order to use {@link ubic.gemma.persistence.service.BaseVoEnabledService#loadValueObjects(Collection)},
     * which is generally more efficient than loading each result individually.
     *
     * @param searchResults a collection of {@link SearchResult}, which may contain a mixture of different {@link Identifiable}
     *                      result objects
     * @return converted search results as per {@link #loadValueObject(SearchResult)}
     * @throws IllegalArgumentException if any of the supplied search results cannot be converted to VO
     * @see ubic.gemma.persistence.service.BaseVoEnabledService#loadValueObjects(Collection)
     */
    List<SearchResult<? extends IdentifiableValueObject<?>>> loadValueObjects( Collection<SearchResult<?>> searchResults ) throws IllegalArgumentException;
}