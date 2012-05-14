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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.expression.experiment.FreeTextExpressionExperimentResultsValueObject;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.util.EntityUtils;

/**
 * Handles searching for experiments and experiment sets
 * 
 * @author tvrossum
 * @version $Id$
 */
@Service
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

    @Autowired
    private ExpressionExperimentSetValueObjectHelper expressionExperimentValueObjectHelper;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService#searchExpressionExperiments(java.lang
     * .String)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        SearchSettings settings = SearchSettings.expressionExperimentSearch( query );
        List<SearchResult> experimentSearchResults = searchService.search( settings ).get( ExpressionExperiment.class );

        if ( experimentSearchResults == null || experimentSearchResults.isEmpty() ) {
            log.info( "No experiments for search: " + query );
            return new HashSet<ExpressionExperimentValueObject>();
        }

        log.info( "Experiment search: " + query + ", " + experimentSearchResults.size() + " found" );
        Collection<ExpressionExperimentValueObject> experimentValueObjects = ExpressionExperimentValueObject
                .convert2ValueObjects( expressionExperimentService.loadMultiple( EntityUtils
                        .getIds( experimentSearchResults ) ) );
        log.info( "Experiment search: " + experimentValueObjects.size() + " value objects returned." );
        return experimentValueObjects;
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller.expression.experiment.ExpressionExperimentController.
     * searchExperimentsAndExperimentGroup(String, Long) does not include session bound sets
     * 
     * @param taxonId
     * @return
     */
    private List<SearchResultDisplayObject> searchExperimentsAndExperimentGroupBlankQuery( Long taxonId ) {
        boolean taxonLimited = ( taxonId != null ) ? true : false;

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();
        List<SearchResultDisplayObject> setResults = new LinkedList<SearchResultDisplayObject>();

        // get all public sets (if user is admin, these were already loaded with
        // expressionExperimentSetService.loadMySets() )
        // filtered by security.
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.loadAllExperimentSetsWithTaxon();
        SearchResultDisplayObject newSRDO = null;
        for ( ExpressionExperimentSet set : sets ) {
            expressionExperimentSetService.thaw( set );
            if ( !taxonLimited || set.getTaxon().getId().equals( taxonId ) ) {
                DatabaseBackedExpressionExperimentSetValueObject eevo = expressionExperimentSetService
                        .getValueObject( set.getId() );
                newSRDO = new SearchResultDisplayObject( eevo );
                newSRDO.setUserOwned( securityService.isPrivate( set ) );
                ( ( ExpressionExperimentSetValueObject ) newSRDO.getResultValueObject() ).setPublik( securityService
                        .isPublic( set ) );
                setResults.add( newSRDO );
            }
        }

        // combine autogen and public results and then sort by taxon
        /*
         * publicResults.addAll( autoGenResults ) Collections.sort( publicResults, new SearchResultTaxonComparator() );
         * displayResults.addAll( publicResults );
         */

        Collections.sort( setResults );
        displayResults.addAll( setResults );

        return displayResults;
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
        boolean taxonLimited = ( taxonId != null ) ? true : false;

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();

        // if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent
        // session-bound sets (not autogen sets until handling of large searches is fixed)
        if ( StringUtils.isBlank( query ) ) {
            return this.searchExperimentsAndExperimentGroupBlankQuery( taxonId );
        }

        // if query is not blank...
        /*
         * GET EXPERIMENTS AND SETS
         */
        SearchSettings settings = SearchSettings.expressionExperimentSearch( query );
        settings.setGeneralSearch( true ); // add a general search
        settings.setSearchExperimentSets( true ); // add searching for experimentSets
        Taxon taxonParam = null;
        if ( taxonLimited ) {
            taxonParam = taxonService.load( taxonId );
            settings.setTaxon( taxonParam );
        }
        Map<Class<?>, List<SearchResult>> results = searchService.search( settings );

        List<SearchResult> eesSR = results.get( ExpressionExperimentSet.class );
        Map<Long, Boolean> isSetOwnedByUser = new HashMap<Long, Boolean>();

        // store userOwned info
        // check if sets are valid for front end
        Collection<SearchResult> toRmvSR = new ArrayList<SearchResult>();
        for ( SearchResult sr : eesSR ) {
            if ( expressionExperimentSetService.isValidForFrontEnd( ( ExpressionExperimentSet ) sr.getResultObject() ) ) {
                ExpressionExperimentSet ees = ( ExpressionExperimentSet ) sr.getResultObject();
                isSetOwnedByUser.put( ees.getId(), securityService.isOwnedByCurrentUser( ees ) );
            } else {
                toRmvSR.add( sr );
            }
        }
        eesSR.removeAll( toRmvSR );

        // get all expressionExperiment results and convert result object into a value object
        List<SearchResult> srEEs = results.get( ExpressionExperiment.class );
        for ( SearchResult sr : srEEs ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) sr.getResultObject();
            ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject( ee );

            Taxon taxon;
            if ( taxonLimited ) {
                // / we we sure this is correct? relies on contract of search to only return results of taxon.
                taxon = taxonParam;
            } else {
                // this would always be safer.
                taxon = expressionExperimentService.getTaxon( ee );
            }

            eevo.setTaxonId( taxon.getId() );
            eevo.setTaxon( taxon.getCommonName() );
            sr.setResultObject( eevo );
        }

        // FIXME this is the second time over this collection.
        // get all expressionExperimentSet results and convert result object into a value object
        for ( SearchResult sr : eesSR ) {
            ExpressionExperimentSet eeSet = ( ExpressionExperimentSet ) sr.getResultObject();
            DatabaseBackedExpressionExperimentSetValueObject eevo = expressionExperimentValueObjectHelper
                    .convertToValueObject( eeSet );
            sr.setResultObject( eevo );
        }

        List<SearchResultDisplayObject> experiments = SearchResultDisplayObject
                .convertSearchResults2SearchResultDisplayObjects( srEEs );
        List<SearchResultDisplayObject> experimentSets = SearchResultDisplayObject
                .convertSearchResults2SearchResultDisplayObjects( eesSR );

        /*
         * THIS SHOULD BE TAKEN CARE OF BY CALL TO 'expressionExperimentSetService.isValidForFrontEnd' BUT THIS NEEDS TO
         * BE VERIFIED
         * 
         * // when searching for an experiment by short name, one or more experiment set(s) is(are) also returned // ex:
         * searching 'GSE2178' gets the experiment and a group called GSE2178 with 1 member // to fix this, if 1 ee is
         * returned and the group only has 1 member and it's member has the // same name as the 1 ee returned, then
         * don't return the ee set if ( experiments.size() == 1 && experimentSets.size() > 0 ) { Long eid = ( (
         * ExpressionExperimentValueObject ) experiments.iterator().next().getResultValueObject() ) .getId();
         * Collection<SearchResultDisplayObject> toRmvSRDO = new ArrayList<SearchResultDisplayObject>(); for (
         * SearchResultDisplayObject srdo : experimentSets ) { if ( srdo.getMemberIds().size() == 1 && (
         * srdo.getMemberIds().toArray() )[0].equals( eid ) ) { toRmvSRDO.add( srdo ); } } experimentSets.removeAll(
         * toRmvSRDO ); }
         */

        // Taxon taxon = null;
        // for each experiment search result display object, set the taxon -- pretty hacky,
        // but only way to get experiment's taxon is using service
        // can I do this in the SRDO constructor?
        // Collection<SearchResultDisplayObject> toRmvSRDO = new ArrayList<SearchResultDisplayObject>();
        // for ( SearchResultDisplayObject srdo : experiments ) {
        //
        // if ( taxonLimited ) {
        // taxon = taxonParam;
        // } else {
        // taxon = expressionExperimentService.getTaxon( ( ( ExpressionExperimentValueObject ) srdo
        // .getResultValueObject() ).getId() );
        // }
        // if ( taxon == null ) {
        // log.warn( "Experiment had null taxon, was excluded from results: experiment id="
        // + ( ( ExpressionExperimentValueObject ) srdo.getResultValueObject() ).getId() + " shortname="
        // + srdo.getName() );
        // toRmvSRDO.add( srdo );
        // } else {
        // srdo.setTaxonId( taxon.getId() );
        // srdo.setTaxonName( taxon.getCommonName() );
        // }
        // }
        // experiments.removeAll( toRmvSRDO );

        // if an eeSet is owned by the user, mark it as such (used for giving it a special background colour in
        // search results)
        if ( SecurityServiceImpl.isUserLoggedIn() ) {
            // tag search result display objects appropriately
            for ( SearchResultDisplayObject srdo : experimentSets ) {
                Long id = ( srdo.getResultValueObject() instanceof DatabaseBackedExpressionExperimentSetValueObject ) ? ( ( ExpressionExperimentSetValueObject ) srdo
                        .getResultValueObject() ).getId() : new Long( -1 );
                srdo.setUserOwned( isSetOwnedByUser.get( id ) );
            }
        }

        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all experiments returned from search
        if ( ( experiments.size() + experimentSets.size() ) > 1 ) {

            // if an experiment was returned by both experiment and experiment set search, don't count it twice
            // (managed by set)
            HashSet<Long> eeIds = new HashSet<Long>();
            HashMap<Long, HashSet<Long>> eeIdsByTaxonId = new HashMap<Long, HashSet<Long>>();

            // add every individual experiment to the set
            for ( SearchResultDisplayObject srdo : experiments ) {
                if ( !eeIdsByTaxonId.containsKey( srdo.getTaxonId() ) ) {
                    eeIdsByTaxonId.put( srdo.getTaxonId(), new HashSet<Long>() );
                }
                ExpressionExperimentValueObject eevo = ( ExpressionExperimentValueObject ) srdo.getResultValueObject();
                eeIdsByTaxonId.get( srdo.getTaxonId() ).add( eevo.getId() );

                eeIds.add( eevo.getId() );
            }

            // if there's a group, get the number of members
            // assuming the taxon of the members is the same as that of the group
            if ( experimentSets.size() > 0 ) {
                // for each group
                for ( SearchResult eesSRO : eesSR ) {
                    ExpressionExperimentSetValueObject set = ( ExpressionExperimentSetValueObject ) eesSRO
                            .getResultObject();
                    Collection<Long> ids = expressionExperimentSetService.getExperimentIdsInSet( set.getId() );
                    // get the ids of the experiment members
                    eeIds.addAll( ids );

                    if ( !eeIdsByTaxonId.containsKey( set.getTaxonId() ) ) {
                        eeIdsByTaxonId.put( set.getTaxonId(), new HashSet<Long>() );
                    }
                    eeIdsByTaxonId.get( set.getTaxonId() ).addAll( ids );
                }
            }

            // make an entry for each taxon

            Long taxonId2 = null;
            for ( Map.Entry<Long, HashSet<Long>> entry : eeIdsByTaxonId.entrySet() ) {
                taxonId2 = entry.getKey();
                Taxon taxon = taxonService.load( taxonId2 );
                if ( taxon != null && entry.getValue().size() > 0 ) {
                    FreeTextExpressionExperimentResultsValueObject ftvo = new FreeTextExpressionExperimentResultsValueObject(
                            "All " + taxon.getCommonName() + " results for '" + query + "'", "All "
                                    + taxon.getCommonName() + " experiments found for your query", taxon.getId(),
                            taxon.getCommonName(), entry.getValue(), query );
                    displayResults.add( new SearchResultDisplayObject( ftvo ) );
                }
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
}
