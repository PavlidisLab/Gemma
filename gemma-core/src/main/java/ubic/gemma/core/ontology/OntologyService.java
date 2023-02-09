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

import org.springframework.beans.factory.InitializingBean;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Paul
 */
@SuppressWarnings("unused") // Possible external use
public interface OntologyService {

    /**
     * Will add the give vocab characteristic to the expression experiment.
     * Does NOT handle persisting of the experiment afterwards.
     *
     * @param vc If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param ee ee
     */
    void addExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee );

    Map<String, CharacteristicValueObject> countObsoleteOccurrences( int start, int stop, int step );

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     *
     * @param  searchQuery           search query
     * @param  useNeuroCartaOntology use neurocarta ontology
     * @return                       characteristic vos
     */
    Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQuery,
            boolean useNeuroCartaOntology ) throws OntologySearchException;

    Collection<OntologyIndividual> findIndividuals( String givenSearch ) throws OntologySearchException;

    /**
     * Given a search string will look through the Mged, birnlex, obo Disease Ontology and FMA Ontology for terms that
     * match the search term. this a lucene backed search, is inexact and for general terms can return a lot of results.
     *
     * @param  search search
     * @return        a collection of Characteristics that are backed by the corresponding found OntologyTerm
     */
    Collection<Characteristic> findTermAsCharacteristic( String search ) throws OntologySearchException;

    /**
     * Given a search string will look through the loaded ontologies for terms that match the search term. If the query
     * looks like a URI, it just retrieves the term.
     * For other queries, this a lucene backed search, is inexact and for general terms can return a lot of results.
     *
     * @param  search search
     * @return        returns a collection of ontologyTerm's
     */
    Collection<OntologyTerm> findTerms( String search ) throws OntologySearchException;

    /**
     * Given a search string will first look through the characteristic database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also. Then will search the loaded ontologies for OntologyResources (Terms and Individuals) that
     * match the search term exactly
     *
     * @param  taxon            Only used if we're going to search for genes or taxon is otherwise relevant; if null,
     *                          restriction is
     *                          not used.
     * @param  givenQueryString query string
     * @return                  characteristic vos
     */
    Collection<CharacteristicValueObject> findTermsInexact( String givenQueryString, Taxon taxon ) throws OntologySearchException, SearchException;

    /**
     * @return terms which are allowed for use in the Category of a Characteristic
     */
    Collection<OntologyTerm> getCategoryTerms();

    /**
     * @return the cellLineOntologyService
     */
    CellLineOntologyService getCellLineOntologyService();

    CellTypeOntologyService getCellTypeOntologyService();

    /**
     * @return the chebiOntologyService
     */
    ChebiOntologyService getChebiOntologyService();

    /**
     * @return the diseaseOntologyService
     */
    DiseaseOntologyService getDiseaseOntologyService();

    /**
     * @return the experimentalFactorOntologyService
     */
    ExperimentalFactorOntologyService getExperimentalFactorOntologyService();

    GemmaOntologyService getGemmaOntologyService();

    HumanDevelopmentOntologyService getHumanDevelopmentOntologyService();

    /**
     * @return the HumanPhenotypeOntologyService
     */
    HumanPhenotypeOntologyService getHumanPhenotypeOntologyService();

    /**
     * @return the MammalianPhenotypeOntologyService
     */
    MammalianPhenotypeOntologyService getMammalianPhenotypeOntologyService();

    MouseDevelopmentOntologyService getMouseDevelopmentOntologyService();

    /**
     * @return the ObiService
     */
    ObiService getObiService();

    /**
     * @param  uri uri
     * @return     the OntologyResource
     */
    OntologyResource getResource( String uri );

    SequenceOntologyService getSequenceOntologyService();

    /**
     * @param  uri uri
     * @return     the OntologyTerm for the specified URI.
     */
    @Nullable
    OntologyTerm getTerm( String uri );

    /**
     * @return UberonService
     */
    UberonOntologyService getUberonService();

    boolean isObsolete( String uri );

    /**
     * Recreate the search indices, for ontologies that are loaded.
     */
    void reindexAllOntologies();

    /**
     * Reinitialize all the ontologies "from scratch". This is necessary if indices are old etc. This should be
     * admin-only.
     */
    void reinitializeAllOntologies();

    void removeBioMaterialStatement( Long characterId, BioMaterial bm );

    /**
     * Will persist the give vocab characteristic to the given biomaterial
     *
     * @param bm bm
     * @param vc vc
     */
    void saveBioMaterialStatement( Characteristic vc, BioMaterial bm );

    void sort( List<CharacteristicValueObject> characteristics );

    Collection<Characteristic> termsToCharacteristics( Collection<? extends OntologyResource> terms );
}