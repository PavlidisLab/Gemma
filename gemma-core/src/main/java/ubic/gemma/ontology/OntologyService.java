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
package ubic.gemma.ontology;

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.BirnLexOntologyService;
import ubic.basecode.ontology.providers.ChebiOntologyService;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.providers.MgedOntologyService;
import ubic.gemma.search.SearchService;

/**
 * @author paul
 * @version $Id$
 */
public interface OntologyService extends InitializingBean {

    /**
     * Given a search string will first look through the characterisc database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also. Then will search the loaded ontologies for OntologyResources (Terms and Individuals) that
     * match the search term exactly
     * 
     * @param givenQueryString
     * @param categoryUri
     * @param taxon Only used if we're going to search for genes or taxon is otherwise relevant.
     * @return
     */
    public abstract Collection<Characteristic> findExactTerm( String givenQueryString, String categoryUri, Taxon taxon );

    /**
     * @param search
     * @return
     */
    public abstract Collection<OntologyIndividual> findIndividuals( String givenSearch );

    /**
     * Given a search string will look through the Mged, birnlex, obo Disease Ontology and FMA Ontology for terms that
     * match the search term. this a lucene backed search, is inexact and for general terms can return alot of results.
     * 
     * @param search
     * @return a collection of VocabCharacteristics that are backed by the corresponding found OntologyTerm
     */
    public abstract Collection<VocabCharacteristic> findTermAsCharacteristic( String search );

    /**
     * Given a search string will look through the loaded ontologies for terms that match the search term. this a lucene
     * backed search, is inexact and for general terms can return a lot of results.
     * 
     * @param search
     * @return returns a collection of ontologyTerm's
     */
    public abstract Collection<OntologyTerm> findTerms( String search );

    /**
     * @return the birnLexOntologyService
     */
    public abstract BirnLexOntologyService getBirnLexOntologyService();

    /**
     * @return the chebiOntologyService
     */
    public abstract ChebiOntologyService getChebiOntologyService();

    /**
     * @return the diseaseOntologyService
     */
    public abstract DiseaseOntologyService getDiseaseOntologyService();

    /**
     * @return the fmaOntologyService
     */
    public abstract FMAOntologyService getFmaOntologyService();

    /**
     * @return the HumanPhenotypeOntologyService
     */
    public abstract HumanPhenotypeOntologyService getHumanPhenotypeOntologyService();

    /**
     * @return the MammalianPhenotypeOntologyService for the specified URI
     */
    public abstract MammalianPhenotypeOntologyService getMammalianPhenotypeOntologyService();

    /**
     * @return the mgedOntologyService
     */
    public abstract MgedOntologyService getMgedOntologyService();

    /**
     * @return the NIFSTDOntologyService
     */
    public abstract NIFSTDOntologyService getNifstfOntologyService();

    /**
     * @return the ObiService for the specified URI
     */
    public abstract ObiService getObiService();

    /**
     * @return the OntologyResource for the specified URI
     */
    public abstract OntologyResource getResource( String uri );

    /**
     * @return the OntologyTerm for the specified URI.
     */
    public abstract OntologyTerm getTerm( String uri );

    /**
     * Reinitialize all the ontologies "from scratch". This is necessary if indices are old etc. This should be
     * admin-only.
     */
    public abstract void reinitializeAllOntologies();

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list.
     * 
     * @param vc
     * @param bmIdList
     */
    public abstract void removeBioMaterialStatement( Collection<Long> characterIds, Collection<Long> bmIdList );

    /**
     * @param characterId characteristic id
     * @param bm
     */
    public abstract void removeBioMaterialStatement( Long characterId, BioMaterial bm );

    /**
     * Will persist the give vocab characteristic to the given biomaterial
     * 
     * @param vc
     * @param bm
     */
    public abstract void saveBioMaterialStatement( Characteristic vc, BioMaterial bm );

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list.
     * 
     * @param vc
     * @param bioMaterialIdList
     */
    public abstract void saveBioMaterialStatement( Characteristic vc, Collection<Long> bioMaterialIdList );

    /**
     * Will persist the give vocab characteristic to the expression experiment.
     * 
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param ee
     */
    public abstract void saveExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee );

    /**
     * @param vc
     * @param ee
     */
    public abstract void saveExpressionExperimentStatements( Collection<Characteristic> vc, ExpressionExperiment ee );

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public abstract void setBioMaterialService( BioMaterialService bioMaterialService );

    /**
     * @param characteristicService the characteristicService to set
     */
    public abstract void setCharacteristicService( CharacteristicService characteristicService );

    /**
     * @param expressionExperimentService
     */
    public abstract void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService );

    /**
     * This is provided for testing purposes. Normally this is set internally.
     * 
     * @param fmaOntologyService the fmaOntologyService to set
     */
    public abstract void setFmaOntologyService( FMAOntologyService fmaOntologyService );

    /**
     * @param searchService the searchService to set
     */
    public abstract void setSearchService( SearchService searchService );

    Collection<CharacteristicValueObject> findExactTermValueObject( String givenQueryString, String categoryUri,
            Taxon taxon );

}