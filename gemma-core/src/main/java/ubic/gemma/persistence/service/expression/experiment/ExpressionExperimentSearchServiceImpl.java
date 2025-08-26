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
package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.search.*;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FreeTextExpressionExperimentResultsValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles searching for experiments and experiment sets
 *
 * @author tvrossum
 */
@Component
public class ExpressionExperimentSearchServiceImpl implements ExpressionExperimentSearchService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentSearchServiceImpl.class );
    private static final String MASTER_SET_PREFIX = "Master set for";
    private static final int MINIMUM_EE_QUERY_LENGTH = 3;

    private final ExpressionExperimentSetService expressionExperimentSetService;
    private final CoexpressionAnalysisService coexpressionAnalysisService;
    private final DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private final SecurityService securityService;
    private final SearchService searchService;
    private final TaxonService taxonService;
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public ExpressionExperimentSearchServiceImpl( ExpressionExperimentSetService expressionExperimentSetService,
            CoexpressionAnalysisService coexpressionAnalysisService,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService,
            SecurityService securityService, SearchService searchService, TaxonService taxonService,
            ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
        this.coexpressionAnalysisService = coexpressionAnalysisService;
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
        this.securityService = securityService;
        this.searchService = searchService;
        this.taxonService = taxonService;
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) throws SearchException {

        SearchSettings settings = SearchSettings.expressionExperimentSearch( query );
        List<SearchResult<ExpressionExperiment>> experimentSearchResults = searchService
                .search( settings )
                .getByResultObjectType( ExpressionExperiment.class );

        if ( experimentSearchResults == null || experimentSearchResults.isEmpty() ) {
            ExpressionExperimentSearchServiceImpl.log.info( "No experiments for search: " + query );
            return new HashSet<>();
        }

        ExpressionExperimentSearchServiceImpl.log
                .info( "Experiment search: " + query + ", " + experimentSearchResults.size() + " found" );
        List<Long> eeIds = experimentSearchResults.stream().map( SearchResult::getResultId ).collect( Collectors.toList() );
        Collection<ExpressionExperimentValueObject> experimentValueObjects = expressionExperimentService
                .loadValueObjectsByIds( experimentSearchResults.stream().map( SearchResult::getResultId ).collect( Collectors.toList() ), true );
        ExpressionExperimentSearchServiceImpl.log
                .info( "Experiment search: " + experimentValueObjects.size() + " value objects returned." );
        return experimentValueObjects;
    }

    @Override
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( List<String> query ) throws SearchException {

        Set<ExpressionExperimentValueObject> all = new HashSet<>();
        Set<ExpressionExperimentValueObject> prev = null;
        Set<ExpressionExperimentValueObject> current;
        for ( String s : query ) {
            s = StringUtils.strip( s );
            if ( prev == null ) {
                prev = new HashSet<>( this.searchExpressionExperiments( s ) );
                all = new HashSet<>( prev );
                continue;
            }
            current = new HashSet<>( this.searchExpressionExperiments( s ) );

            all = SetUtils.intersection( all, current );
        }
        return all;
    }

    @Override
    public List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) throws SearchException {

        List<SearchResultDisplayObject> displayResults = new LinkedList<>();

        // if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent
        // session-bound sets (not autogen sets until handling of large searches is fixed)
        if ( StringUtils.isBlank( query ) ) {
            return this.searchExperimentsAndExperimentGroupBlankQuery( taxonId );
        }

        SearchService.SearchResultMap results = this.initialSearch( query, taxonId );

        List<SearchResultDisplayObject> experimentSets = this.getExpressionExperimentSetResults( results );
        List<SearchResultDisplayObject> experiments = this.getExpressionExperimentResults( results );

        if ( experimentSets.isEmpty() && experiments.isEmpty() ) {
            return displayResults;
        }

        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all experiments returned from search

        Map<Long, Set<Long>> eeIdsByTaxonId = new HashMap<>();

        // add every individual experiment to the set, grouped by taxon and also altogether.
        for ( SearchResultDisplayObject srdo : experiments ) {

            Long taxId = srdo.getTaxonId();

            if ( !eeIdsByTaxonId.containsKey( taxId ) ) {
                eeIdsByTaxonId.put( taxId, new HashSet<Long>() );
            }
            ExpressionExperimentValueObject eevo = ( ExpressionExperimentValueObject ) srdo.getResultValueObject();
            eeIdsByTaxonId.get( taxId ).add( eevo.getId() );
        }

        // if there's a group, get the number of members
        // assuming the taxon of the members is the same as that of the group

        // for each group
        for ( SearchResultDisplayObject eesSRO : experimentSets ) {
            ExpressionExperimentSetValueObject set = ( ExpressionExperimentSetValueObject ) eesSRO
                    .getResultValueObject();

            /*
             * This is security filtered.
             */
            Collection<Long> ids = IdentifiableUtils
                    .getIds( expressionExperimentSetService.getExperimentValueObjectsInSet( set.getId() ) );

            set.setSize( ids.size() ); // to account for security filtering.

            if ( !eeIdsByTaxonId.containsKey( set.getTaxonId() ) ) {
                eeIdsByTaxonId.put( set.getTaxonId(), new HashSet<Long>() );
            }
            eeIdsByTaxonId.get( set.getTaxonId() ).addAll( ids );
        }

        // make an entry for each taxon

        Long taxonId2;
        for ( Map.Entry<Long, Set<Long>> entry : eeIdsByTaxonId.entrySet() ) {
            taxonId2 = entry.getKey();
            Taxon taxon = taxonService.load( taxonId2 );
            if ( taxon != null && entry.getValue().size() > 0 ) {

                FreeTextExpressionExperimentResultsValueObject ftvo = new FreeTextExpressionExperimentResultsValueObject(
                        "All " + taxon.getCommonName() + " results for '" + query + "'",
                        "All " + taxon.getCommonName() + " experiments found for your query", taxon.getId(),
                        taxon.getCommonName(), entry.getValue(), query );

                int numWithDifferentialExpressionAnalysis = differentialExpressionAnalysisService
                        .getExperimentsWithAnalysis( entry.getValue(), true ).size();

                assert numWithDifferentialExpressionAnalysis <= entry.getValue().size();

                int numWithCoexpressionAnalysis = coexpressionAnalysisService
                        .getExperimentsWithAnalysis( entry.getValue() ).size();

                ftvo.setNumWithCoexpressionAnalysis( numWithCoexpressionAnalysis );
                ftvo.setNumWithDifferentialExpressionAnalysis( numWithDifferentialExpressionAnalysis );
                displayResults.add( new SearchResultDisplayObject( ftvo ) );
            }
        }

        displayResults.addAll( experimentSets );
        displayResults.addAll( experiments );

        if ( displayResults.isEmpty() ) {
            ExpressionExperimentSearchServiceImpl.log.info( "No results for search: " + query );
        } else {
            ExpressionExperimentSearchServiceImpl.log
                    .info( "Results for search: " + query + " size=" + displayResults.size() + " entry0: "
                            + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getName()
                            + " valueObject:" + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] )
                            .getResultValueObject().toString() );
        }
        return displayResults;
    }

    @Override
    public List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId ) {

        List<SearchResultDisplayObject> setResults = new LinkedList<>();

        Taxon taxon = taxonService.loadOrFail( taxonId );

        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService
                .findByName( "Master set for " + taxon.getCommonName().toLowerCase() );
        SearchResultDisplayObject newSRDO;
        for ( ExpressionExperimentSet set : sets ) {
            set = expressionExperimentSetService.thaw( set );
            if ( set.getTaxon().getId().equals( taxonId ) ) {
                ExpressionExperimentSetValueObject eevo = expressionExperimentSetService.loadValueObject( set );
                if ( eevo != null ) {
                    newSRDO = new SearchResultDisplayObject( eevo );
                    newSRDO.setUserOwned( securityService.isPrivate( set ) );
                    ( ( ExpressionExperimentSetValueObject ) newSRDO.getResultValueObject() )
                            .setIsPublic( securityService.isPublic( set ) );
                    setResults.add( newSRDO );
                }
            }
        }

        Collections.sort( setResults );

        return setResults;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> searchExpressionExperiments( String query, @Nullable Long taxonId ) throws SearchException {
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        Collection<Long> eeIds = new HashSet<>();
        if ( StringUtils.isNotBlank( query ) ) {

            if ( query.length() < MINIMUM_EE_QUERY_LENGTH )
                return eeIds;

            // Initial list
            SearchSettings settings = SearchSettings.expressionExperimentSearch( query, taxon );
            /* no fill */
            /*
             * speed
             * search,
             * irrelevant
             */
            List<SearchResult<ExpressionExperiment>> results = searchService.search( settings.withFillResults( false ).withMode( SearchSettings.SearchMode.BALANCED ) )
                    .getByResultObjectType( ExpressionExperiment.class );
            for ( SearchResult<ExpressionExperiment> result : results ) {
                eeIds.add( result.getResultId() );
            }
        } else if ( taxon != null ) {
            // get all for taxon
            eeIds = IdentifiableUtils.getIds( expressionExperimentService.findByTaxon( taxon ) );
        }
        return eeIds;
    }

    private List<SearchResultDisplayObject> getExpressionExperimentResults( SearchService.SearchResultMap results ) {
        // get all expressionExperiment results and convert result object into a value object
        List<SearchResult<ExpressionExperiment>> srEEs = results.getByResultObjectType( ExpressionExperiment.class );

        List<Long> eeIds = new ArrayList<>();
        for ( SearchResult<ExpressionExperiment> sr : srEEs ) {
            eeIds.add( sr.getResultId() );
        }

        Collection<ExpressionExperimentValueObject> eevos = expressionExperimentService.loadValueObjectsByIds( eeIds, true );
        List<SearchResultDisplayObject> experiments = new ArrayList<>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            experiments.add( new SearchResultDisplayObject( eevo ) );
        }
        return experiments;
    }

    private List<SearchResultDisplayObject> getExpressionExperimentSetResults(
            SearchService.SearchResultMap results ) {
        List<SearchResultDisplayObject> experimentSets = new ArrayList<>();

        List<Long> eeSetIds = new ArrayList<>();
        for ( SearchResult<ExpressionExperimentSet> sr : results.getByResultObjectType( ExpressionExperimentSet.class ) ) {
            eeSetIds.add( sr.getResultId() );
        }

        if ( eeSetIds.isEmpty() ) {
            return experimentSets;
        }
        for ( ExpressionExperimentSetValueObject eesvo : expressionExperimentSetService
                .loadValueObjectsByIds( eeSetIds ) ) {
            experimentSets.add( new SearchResultDisplayObject( eesvo ) );
        }
        return experimentSets;
    }

    private SearchService.SearchResultMap initialSearch( String query, @Nullable Long taxonId ) throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( query )
                .resultType( ExpressionExperiment.class )
                .resultType( ExpressionExperimentSet.class ) // add searching for experimentSets
                .build();
        Taxon taxonParam;
        if ( taxonId != null ) {
            taxonParam = taxonService.load( taxonId );
            settings.setTaxonConstraint( taxonParam );
        }
        return searchService.search( settings );
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller .expression.experiment.ExpressionExperimentController.
     * searchExperimentsAndExperimentGroup(String, Long) does not include session bound sets
     */
    private List<SearchResultDisplayObject> searchExperimentsAndExperimentGroupBlankQuery( Long taxonId ) {
        boolean taxonLimited = taxonId != null;

        List<SearchResultDisplayObject> displayResults = new LinkedList<>();

        // These are widely considered to be the most important results and
        // therefore need to be at the top
        List<SearchResultDisplayObject> masterResults = new LinkedList<>();

        Collection<ExpressionExperimentSetValueObject> evos = expressionExperimentSetService
                .loadAllExperimentSetValueObjects( true );

        for ( ExpressionExperimentSetValueObject evo : evos ) {

            if ( taxonLimited && !evo.getTaxonId().equals( taxonId ) ) {
                continue;
            }

            SearchResultDisplayObject srdvo = new SearchResultDisplayObject( evo );
            if ( evo.getName().startsWith( ExpressionExperimentSearchServiceImpl.MASTER_SET_PREFIX ) ) {
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
