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

import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.genome.Gene;

import java.util.*;

/**
 * @author paul
 */
public interface SearchService {

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
     * @param  settings settings
     * @return Map of Class to SearchResults. The results are already filtered for security considerations.
     */
    Map<Class<?>, List<SearchResult<?>>> search( SearchSettings settings ) throws SearchException;

    /**
     * This speedSearch method is probably unnecessary right now considering we only call from geneSearch, just putting
     * it in
     * because we probably want to use something like this on the general search page
     *
     * @param  settings settings
     * @return Map of Class to SearchResults. The results are already filtered for security considerations.
     * @see             #search(SearchSettings)
     */
    Map<Class<?>, List<SearchResult<?>>> speedSearch( SearchSettings settings ) throws SearchException;

    /**
     * Makes an attempt at determining of the query term is a valid URI from an Ontology in Gemma or a Gene URI (a GENE
     * URI is in the form: http://purl.org/commons/record/ncbi_gene/20655 (but is not a valid ontology loaded in gemma)
     * ). If so then searches for objects that have been tagged with that term or any of that terms children. If not a
     * URI then proceeds with the generalSearch.
     *
     * @param  fillObjects    If false, the entities will not be filled in inside the SearchSettings; instead, they will
     *                        be
     *                        nullified (for security purposes). You can then use the id and Class stored in the
     *                        SearchSettings to load the
     *                        entities at your leisure. If true, the entities are loaded in the usual secure fashion.
     *                        Setting this to
     *                        false can be an optimization if all you need is the id.
     * @param  webSpeedSearch If true, the search will be faster but the results may not be as broad as when this is
     *                        false.
     *                        Set to true for frontend combo boxes like the gene combo
     * @param  settings       settings
     * @return Map of Class to SearchResults. The results are already filtered for security
     *                        considerations.
     */
    Map<Class<?>, List<SearchResult<?>>> search( SearchSettings settings, boolean fillObjects, boolean webSpeedSearch ) throws SearchException;

    /**
     * A search of experiments only. At least one of the arguments must be non-null.
     *
     * @param  query   if non-blank, used to search for experiments; filters and limits may be applied.
     * @param  taxonId can be null; if non-null, used to filter results. If query is blank, all experiments for the
     *                 taxon are returned.
     * @return Collection of ids.
     */
    Collection<Long> searchExpressionExperiments( String query, Long taxonId ) throws SearchException;

    /**
     * convenience method to return only search results from one class
     *
     * @param  <T>
     *
     * @param  settings    settings
     * @param  resultClass class
     * @return only search results from one class
     */
    <T> List<T> search( SearchSettings settings, Class<T> resultClass ) throws SearchException;

    /**
     * Returns a set of supported result types.
     *
     * This is mainly used to perform a search for everything via {@link SearchSettings#getResultTypes()}.
     */
    Set<Class<?>> getSupportedResultTypes();

    /**
     * Convert the {@link SearchResult#getResultObject()} to its corresponding VO representation.
     */
    Object convertSearchResultObjectToValueObject( SearchResult searchResult );
}