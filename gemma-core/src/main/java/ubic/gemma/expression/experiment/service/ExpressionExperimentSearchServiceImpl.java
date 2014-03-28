/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package ubic.gemma.expression.experiment.service;

import gemma.gsec.SecurityService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.expression.experiment.FreeTextExpressionExperimentResultsValueObject;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.EntityUtils;

/**
 * Handles searching for experiments and experiment sets
 * 
 * @author tvrossum
 * @version $Id$
 */
@Component
public class ExpressionExperimentSearchServiceImpl implements ExpressionExperimentSearchService {

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private static String arbitraryMasterSetPrefix = "Master set for";

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService#getAllTaxonExperimentGroup(java.lang
     * .Long)
     */
    @Override
    public List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId ) {

        List<SearchResultDisplayObject> setResults = new LinkedList<SearchResultDisplayObject>();

        Taxon taxon = taxonService.load( taxonId );

        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( "Master set for "
                + taxon.getCommonName().toLowerCase() );
        SearchResultDisplayObject newSRDO = null;
        for ( ExpressionExperimentSet set : sets ) {
            expressionExperimentSetService.thaw( set );
            if ( set.getTaxon().getId().equals( taxonId ) ) {
                ExpressionExperimentSetValueObject eevo = expressionExperimentSetService.loadValueObject( set.getId() );
                newSRDO = new SearchResultDisplayObject( eevo );
                newSRDO.setUserOwned( securityService.isPrivate( set ) );
                ( ( ExpressionExperimentSetValueObject ) newSRDO.getResultValueObject() ).setIsPublic( securityService
                        .isPublic( set ) );
                setResults.add( newSRDO );
            }
        }

        Collections.sort( setResults );

        return setResults;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService#searchExperimentsAndExperimentGroups
     * (java.lang.String, java.lang.Long)
     */
    @Override
    public List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) {

        List<SearchResultDisplayObject> displayResults = new LinkedList<>();

        // if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent
        // session-bound sets (not autogen sets until handling of large searches is fixed)
        if ( StringUtils.isBlank( query ) ) {
            return this.searchExperimentsAndExperimentGroupBlankQuery( taxonId );
        }

        Map<Class<?>, List<SearchResult>> results = initialSearch( query, taxonId );

        List<SearchResultDisplayObject> experimentSets = getExpressionExperimentSetResults( results );
        List<SearchResultDisplayObject> experiments = getExpressionExperimentResults( results );

        if ( experimentSets.isEmpty() && experiments.isEmpty() ) {
            return displayResults;
        }

        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all experiments returned from search

        // if an experiment was returned by both experiment and experiment set search, don't count it twice
        // (managed by set)
        Set<Long> eeIds = new HashSet<>();
        Map<Long, HashSet<Long>> eeIdsByTaxonId = new HashMap<>();

        // add every individual experiment to the set, grouped by taxon and also altogether.
        for ( SearchResultDisplayObject srdo : experiments ) {

            // group by the Parent Taxon, for things like salmonid - see bug 3286
            Long taxId = null;
            if ( srdo.getParentTaxonId() != null ) {
                taxId = srdo.getParentTaxonId();
            } else {
                taxId = srdo.getTaxonId();
            }

            if ( !eeIdsByTaxonId.containsKey( taxId ) ) {
                eeIdsByTaxonId.put( taxId, new HashSet<Long>() );
            }
            ExpressionExperimentValueObject eevo = ( ExpressionExperimentValueObject ) srdo.getResultValueObject();
            eeIdsByTaxonId.get( taxId ).add( eevo.getId() );
            eeIds.add( eevo.getId() );
        }

        // if there's a group, get the number of members
        // assuming the taxon of the members is the same as that of the group

        // for each group
        for ( SearchResultDisplayObject eesSRO : experimentSets ) {
            ExpressionExperimentSetValueObject set = ( ExpressionExperimentSetValueObject ) eesSRO
                    .getResultValueObject();

            /*
             * Small problem: this is not security-filtered. See bug 3385; if the set is public, but some of the
             * experiments in the set are not, the ID will be included.
             */
            Collection<Long> ids = EntityUtils.getIds( expressionExperimentSetService
                    .getExperimentValueObjectsInSet( set.getId() ) );
            // get the ids of the experiment members
            eeIds.addAll( ids );

            if ( !eeIdsByTaxonId.containsKey( set.getTaxonId() ) ) {
                eeIdsByTaxonId.put( set.getTaxonId(), new HashSet<Long>() );
            }
            eeIdsByTaxonId.get( set.getTaxonId() ).addAll( ids );
        }

        // make an entry for each taxon

        Long taxonId2 = null;
        for ( Map.Entry<Long, HashSet<Long>> entry : eeIdsByTaxonId.entrySet() ) {
            taxonId2 = entry.getKey();
            Taxon taxon = taxonService.load( taxonId2 );
            if ( taxon != null && entry.getValue().size() > 0 ) {

                FreeTextExpressionExperimentResultsValueObject ftvo = new FreeTextExpressionExperimentResultsValueObject(
                        "All " + taxon.getCommonName() + " results for '" + query + "'", "All " + taxon.getCommonName()
                                + " experiments found for your query", taxon.getId(), taxon.getCommonName(),
                        entry.getValue(), query );
                displayResults.add( new SearchResultDisplayObject( ftvo ) );
            }
        }

        displayResults.addAll( experimentSets );
        displayResults.addAll( experiments );

        if ( displayResults.isEmpty() ) {
            log.info( "No results for search: " + query );
        } else {
            log.info( "Results for search: "
                    + query
                    + " size="
                    + displayResults.size()
                    + " entry0: "
                    + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getName()
                    + " valueObject:"
                    + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getResultValueObject()
                            .toString() );
        }
        return displayResults;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService#searchExpressionExperiments(java.lang
     * .String)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        SearchSettings settings = SearchSettingsImpl.expressionExperimentSearch( query );
        List<SearchResult> experimentSearchResults = searchService.search( settings ).get( ExpressionExperiment.class );

        if ( experimentSearchResults == null || experimentSearchResults.isEmpty() ) {
            log.info( "No experiments for search: " + query );
            return new HashSet<ExpressionExperimentValueObject>();
        }

        log.info( "Experiment search: " + query + ", " + experimentSearchResults.size() + " found" );
        Collection<ExpressionExperimentValueObject> experimentValueObjects = expressionExperimentService
                .loadValueObjects( EntityUtils.getIds( experimentSearchResults ), true );
        log.info( "Experiment search: " + experimentValueObjects.size() + " value objects returned." );
        return experimentValueObjects;
    }

    /**
     * @param results
     * @return
     */
    private List<SearchResultDisplayObject> getExpressionExperimentResults( Map<Class<?>, List<SearchResult>> results ) {
        // get all expressionExperiment results and convert result object into a value object
        List<SearchResult> srEEs = results.get( ExpressionExperiment.class );
        if ( srEEs == null ) {
            srEEs = new ArrayList<SearchResult>();
        }

        List<Long> eeIds = new ArrayList<Long>();
        for ( SearchResult sr : srEEs ) {
            eeIds.add( sr.getId() );
        }

        Collection<ExpressionExperimentValueObject> eevos = expressionExperimentService.loadValueObjects( eeIds, true );
        List<SearchResultDisplayObject> experiments = new ArrayList<SearchResultDisplayObject>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            experiments.add( new SearchResultDisplayObject( eevo ) );
        }
        return experiments;
    }

    /**
     * @param results
     * @return
     */
    private List<SearchResultDisplayObject> getExpressionExperimentSetResults( Map<Class<?>, List<SearchResult>> results ) {
        List<SearchResultDisplayObject> experimentSets = new ArrayList<SearchResultDisplayObject>();

        if ( results.get( ExpressionExperimentSet.class ) != null ) {
            List<Long> eeSetIds = new ArrayList<Long>();
            for ( SearchResult sr : results.get( ExpressionExperimentSet.class ) ) {
                eeSetIds.add( ( ( ExpressionExperimentSet ) sr.getResultObject() ).getId() );
            }

            if ( eeSetIds.isEmpty() ) {
                return experimentSets;
            }

            for ( ExpressionExperimentSetValueObject eesvo : expressionExperimentSetService.loadValueObjects( eeSetIds ) ) {
                experimentSets.add( new SearchResultDisplayObject( eesvo ) );
            }
        }
        return experimentSets;
    }

    /**
     * @param query
     * @param taxonId
     * @return
     */
    private Map<Class<?>, List<SearchResult>> initialSearch( String query, Long taxonId ) {
        SearchSettings settings = SearchSettingsImpl.expressionExperimentSearch( query );
        settings.setSearchExperimentSets( true ); // add searching for experimentSets
        Taxon taxonParam = null;
        if ( taxonId != null ) {
            taxonParam = taxonService.load( taxonId );
            settings.setTaxon( taxonParam );
        }
        Map<Class<?>, List<SearchResult>> results = searchService.search( settings );
        return results;
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller .expression.experiment.ExpressionExperimentController.
     * searchExperimentsAndExperimentGroup(String, Long) does not include session bound sets
     * 
     * @param taxonId
     * @return
     */
    private List<SearchResultDisplayObject> searchExperimentsAndExperimentGroupBlankQuery( Long taxonId ) {
        boolean taxonLimited = taxonId != null;

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();

        // These are widely considered to be the most important results and
        // therefore need to be at the top
        List<SearchResultDisplayObject> masterResults = new LinkedList<SearchResultDisplayObject>();

        Collection<ExpressionExperimentSetValueObject> evos = expressionExperimentSetService
                .loadAllExperimentSetValueObjects();

        for ( ExpressionExperimentSetValueObject evo : evos ) {
            SearchResultDisplayObject srdvo = new SearchResultDisplayObject( evo );

            if ( taxonLimited && !evo.getTaxonId().equals( taxonId ) ) {
                continue;
            }
            // could be spoofed by other users 'Master sets'
            if ( evo.getName().startsWith( arbitraryMasterSetPrefix ) ) {
                masterResults.add( srdvo );
            } else {
                displayResults.add( srdvo );
            }
        }

        Collections.sort( displayResults );

        // should we also sort by which species is most important(humans obviously) or is that not politically
        // correct???
        displayResults.addAll( 0, masterResults );

        return displayResults;
    }
}
