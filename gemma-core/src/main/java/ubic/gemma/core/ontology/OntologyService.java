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
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.Collection;

/**
 * @author Paul
 */
public interface OntologyService extends InitializingBean {

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     */
    Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQuery,
            boolean useNeuroCartaOntology );

    Collection<OntologyIndividual> findIndividuals( String givenSearch );

    /**
     * Given a search string will look through the Mged, birnlex, obo Disease Ontology and FMA Ontology for terms that
     * match the search term. this a lucene backed search, is inexact and for general terms can return a lot of results.
     *
     * @return a collection of VocabCharacteristics that are backed by the corresponding found OntologyTerm
     */
    Collection<VocabCharacteristic> findTermAsCharacteristic( String search );

    /**
     * Given a search string will look through the loaded ontologies for terms that match the search term. this a lucene
     * backed search, is inexact and for general terms can return a lot of results.
     *
     * @return returns a collection of ontologyTerm's
     */
    Collection<OntologyTerm> findTerms( String search );

    /**
     * Given a search string will first look through the characteristic database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also. Then will search the loaded ontologies for OntologyResources (Terms and Individuals) that
     * match the search term exactly
     *
     * @param taxon Only used if we're going to search for genes or taxon is otherwise relevant; if null, restriction is
     *              not used.
     */
    Collection<CharacteristicValueObject> findTermsInexact( String givenQueryString, Taxon taxon );

    /**
     * @return terms which are allowed for use in the Category of a Characteristic
     */
    Collection<OntologyTerm> getCategoryTerms();

    /**
     * @return the cellLineOntologyService
     */
    CellLineOntologyService getCellLineOntologyService();

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

    /**
     * @return the fmaOntologyService
     */
    FMAOntologyService getFmaOntologyService();

    /**
     * @return the HumanPhenotypeOntologyService
     */
    HumanPhenotypeOntologyService getHumanPhenotypeOntologyService();

    /**
     * @return the MammalianPhenotypeOntologyService for the specified URI
     */
    MammalianPhenotypeOntologyService getMammalianPhenotypeOntologyService();

    /**
     * @return the NIFSTDOntologyService
     */
    NIFSTDOntologyService getNifstfOntologyService();

    /**
     * @return the ObiService for the specified URI
     */
    ObiService getObiService();

    /**
     * @return the OntologyResource for the specified URI
     */
    OntologyResource getResource( String uri );

    SequenceOntologyService getSequenceOntologyService();

    /**
     * @return the OntologyTerm for the specified URI.
     */
    OntologyTerm getTerm( String uri );

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
     */
    void saveBioMaterialStatement( Characteristic vc, BioMaterial bm );

    /**
     * Will persist the give vocab characteristic to the expression experiment.
     *
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     */
    void saveExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee );

    void saveExpressionExperimentStatements( Collection<Characteristic> vc, ExpressionExperiment ee );

}