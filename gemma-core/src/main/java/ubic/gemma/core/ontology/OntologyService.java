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
package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Paul
 */
@SuppressWarnings("unused") // Possible external use
public interface OntologyService {

    /**
     * Locates usages of obsolete terms in Characteristics, ignoring Gene Ontology annotations. Requires the ontologies are loaded into memory.
     * <p>
     * Will also find terms that are no longer in an ontology we use.
     * @return map of value URI to a representative characteristic using the term. The latter will contain a count
     * of how many occurrences there were.
     */
    Map<Characteristic, Long> findObsoleteTermUsage();

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     *
     * @param  searchQuery           search query
     * @param  useNeuroCartaOntology use neurocarta ontology
     * @return characteristic vos
     */
    @Deprecated
    Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQuery,
            boolean useNeuroCartaOntology ) throws SearchException;

    /**
     * Given a search string will look through the loaded ontologies for terms that match the search term. If the query
     * looks like a URI, it just retrieves the term.
     * For other queries, this a lucene backed search, is inexact and for general terms can return a lot of results.
     *
     * @param  query search query
     * @return returns a collection of ontologyTerm's
     */
    Collection<OntologyTerm> findTerms( String query ) throws SearchException;

    /**
     * Given a search string will first look through the characteristic database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also.
     *
     * @param  taxon            Only used if we're going to search for genes or taxon is otherwise relevant; if null,
     *                          restriction is
     *                          not used.
     * @param  givenQueryString query string
     * @return characteristic vos
     */
    Collection<CharacteristicValueObject> findTermsInexact( String givenQueryString, @Nullable Taxon taxon ) throws SearchException;

    /**
     * Obtain terms which are allowed for use in the category of a {@link ubic.gemma.model.common.description.Characteristic}.
     */
    Set<OntologyTerm> getCategoryTerms();

    /**
     * Obtain terms allowed for the predicate (relationship) in a {@link ubic.gemma.model.expression.experiment.Statement}.
     */
    Set<OntologyProperty> getRelationTerms();

    /**
     * Obtain the parents of a collection of terms.
     * @see OntologyTerm#getParents(boolean, boolean)
     */
    Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties );

    /**
     * Obtain the children of a collection of terms.
     * @see OntologyTerm#getChildren(boolean, boolean)
     */
    Set<OntologyTerm> getChildren( Collection<OntologyTerm> matchingTerms, boolean direct, boolean includeAdditionalProperties );

    /**
     * Obtain a definition for the given URI.
     */
    @Nullable
    String getDefinition( String uri );

    /**
     * Obtain a term for the given URI.
     */
    @Nullable
    OntologyTerm getTerm( String uri );

    /**
     * Return all the terms matching the given URIs.
     */
    Set<OntologyTerm> getTerms( Collection<String> uris );

    /**
     * Recreate the search indices, for ontologies that are loaded.
     */
    void reindexAllOntologies();

    /**
     * Reinitialize (and reindex) all the ontologies "from scratch". This is necessary if indices are old etc. This should be
     * admin-only.
     */
    void reinitializeAndReindexAllOntologies();
}