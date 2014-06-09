/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionSearchCommand;
import ubic.gemma.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionSearchService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.tasks.AbstractTask;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.Settings;

/**
 * @author luke
 * @version $Id$
 */
@Controller
public class CoexpressionSearchController {

    /**
     * Inner class used for doing a long running coex search
     */
    class CoexpressionSearchTask extends AbstractTask<TaskResult, TaskCommand> {

        public CoexpressionSearchTask( CoexSearchTaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {

            CoexSearchTaskCommand coexCommand = ( CoexSearchTaskCommand ) taskCommand;

            CoexpressionMetaValueObject results = doSearch( coexCommand.getSearchOptions() );

            return new TaskResult( taskCommand, results );

        }
    }

    private static final int DEFAULT_MAX_GENES_PER_MY_GENES_ONLY = 500;

    private static final int DEFAULT_MAX_RESULTS = 200;

    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private static final int MAX_GENES_FOR_QUERY_GENES_ONLY_QUERY = Settings.getInt(
            "gemma.coexpressionSearch.maxGenesForQueryGenesOnly", DEFAULT_MAX_GENES_PER_MY_GENES_ONLY );

    private static final int MAX_RESULTS_PER_GENE = Settings.getInt( "gemma.coexpressionSearch.maxResultsPerQueryGene",
            DEFAULT_MAX_RESULTS );

    /**
     * 
     */
    private static final String NOTHING_FOUND_MESSAGE = "No coexpression found with those settings";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private GeneCoexpressionSearchService geneCoexpressionService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaskRunningService taskRunningService;

    /**
     * Used by CoexpressionSearchData.js - the main home page coexpression search.
     * 
     * @param searchOptions
     * @return
     */
    public String doBackgroundCoexSearch( CoexpressionSearchCommand searchOptions ) {
        CoexSearchTaskCommand options = new CoexSearchTaskCommand( searchOptions );
        CoexpressionSearchTask job = new CoexpressionSearchTask( options );
        return taskRunningService.submitLocalTask( job );
    }

    @Autowired
    private GeneSetService geneSetService;

    /**
     * Important entry point - called by the CoexpressionSearchTask
     * 
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {

        Collection<ExpressionExperiment> myEE = null;
        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();

        if ( searchOptions.getGeneIds() == null || searchOptions.getGeneIds().isEmpty() ) {

            if ( searchOptions.getGeneSetId() != null ) {
                searchOptions.setGeneIds( geneSetService.getGeneIdsInGroup( new GeneSetValueObject( searchOptions
                        .getGeneSetId() ) ) );
            }

            if ( searchOptions.getGeneIds().isEmpty() ) {
                result.setErrorState( "No genes were selected" );
                return result;
            }
        }

        if ( searchOptions.getGeneIds().size() > MAX_GENES_FOR_QUERY_GENES_ONLY_QUERY ) {
            result.setErrorState( "Too many genes selected, please limit searches to "
                    + MAX_GENES_FOR_QUERY_GENES_ONLY_QUERY + " genes" );
            return result;
        }

        if ( searchOptions.getGeneIds().size() == 0 ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;
        }

        Collection<Long> eeIds = new HashSet<>();
        Long eeSetId = null;
        if ( searchOptions.getEeIds() != null && !searchOptions.getEeIds().isEmpty() ) {
            // security filter.
            eeIds = EntityUtils.getIds( expressionExperimentService.loadMultiple( searchOptions.getEeIds() ) );
        } else if ( searchOptions.getEeSetId() != null ) {
            eeSetId = searchOptions.getEeSetId();
        } else if ( StringUtils.isNotBlank( searchOptions.getEeSetName() ) ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                    .getEeSetName() );
            if ( eeSets.size() == 1 ) {
                eeSetId = eeSets.iterator().next().getId();
            } else {
                result.setErrorState( "Unknown or ambiguous set name: " + searchOptions.getEeSetName() );
                return result;
            }
        }

        /*
         * Got an ee set.
         */
        if ( eeSetId != null ) {
            eeIds = EntityUtils.getIds( expressionExperimentSetService.getExperimentsInSet( eeSetId ) );
        }

        // Add the user's datasets to the selected datasets
        if ( searchOptions.isUseMyDatasets() ) {
            myEE = expressionExperimentService.loadMyExpressionExperiments();
            if ( myEE != null && !myEE.isEmpty() ) {
                for ( ExpressionExperiment ee : myEE ) {
                    eeIds.add( ee.getId() );
                }
            } else
                log.info( "No user data to add" );
        }

        if ( eeIds.isEmpty() ) {
            // search all available experiments.
        } else {
            searchOptions.setEeIds( eeIds );
        }

        restrictSearchOptionsQueryGenes( searchOptions );
        log.info( "Starting coexpression search: " + searchOptions );

        result = geneCoexpressionService.coexpressionSearch( searchOptions.getEeIds(), searchOptions.getGeneIds(),
                searchOptions.getStringency(), MAX_RESULTS_PER_GENE, searchOptions.getQueryGenesOnly() );

        // FIXME This is ugly - we need to consolidate some of this.
        if ( result.getTrimStringency() > searchOptions.getStringency() ) {
            searchOptions.setStringency( result.getTrimStringency() );
        }
        result.setSearchSettings( searchOptions );
        result.trim();

        if ( searchOptions.isUseMyDatasets() ) {
            addMyDataFlag( result, myEE );
        }

        // make sure to create an empty list instead of null for front-end
        if ( result.getResults() == null ) {
            List<CoexpressionValueObjectExt> res = new ArrayList<>();
            result.setResults( res );
        }

        if ( result.getResults().isEmpty() ) {
            result.setErrorState( NOTHING_FOUND_MESSAGE );
            log.info( "No search results for query: " + searchOptions );
        }

        return result;

    }

    /**
     * Do a search that fills in the edges among the genes already found. Maps to doSearchQuickComplete in javascript.
     * 
     * @param searchOptions
     * @param queryGeneIds the genes which were used originally.
     * @return
     */
    public CoexpressionMetaValueObject doSearchQuickComplete( CoexpressionSearchCommand searchOptions,
            Collection<Long> queryGeneIds ) {

        if ( searchOptions == null ) {
            throw new IllegalArgumentException( "Search options cannot be null" );
        }

        assert queryGeneIds != null && !queryGeneIds.isEmpty();
        assert searchOptions.getQueryGenesOnly();

        restrictSearchOptionsQueryGenes( searchOptions );

        StopWatch timer = new StopWatch();
        timer.start();

        log.info( "=== Graph-completion coexpression search for " + searchOptions.getGeneIds().size()
                + " genes, stringency=" + searchOptions.getStringency() + " eeset=" + searchOptions.getEeSetId() );

        CoexpressionMetaValueObject result = geneCoexpressionService.coexpressionSearchQuick( searchOptions.getEeIds(),
                searchOptions.getGeneIds(), searchOptions.getStringency(), MAX_RESULTS_PER_GENE, true );

        result.setSearchSettings( searchOptions );
        result.trim( new HashSet<>( queryGeneIds ) );

        if ( result.getResults() == null || result.getResults().isEmpty() ) {
            result.setErrorState( NOTHING_FOUND_MESSAGE );
            log.info( "No search results for query: " + searchOptions );
        }

        if ( timer.getTime() > 2000 ) {
            log.info( "==== Search complete: " + result.getResults().size() + " hits" );
        }

        return result;
    }

    /**
     * @param vo
     * @param eesToFlag
     */
    private void addMyDataFlag( CoexpressionMetaValueObject vo, Collection<ExpressionExperiment> eesToFlag ) {

        Collection<Long> eesToFlagIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : eesToFlag ) {
            eesToFlagIds.add( ee.getId() );
        }

        if ( vo == null || vo.getResults() == null || vo.getResults().isEmpty() ) return;

        for ( CoexpressionValueObjectExt covo : vo.getResults() ) {
            for ( Long eeToFlag : eesToFlagIds ) {
                if ( covo.getSupportingExperiments().contains( eeToFlag ) ) {
                    covo.setContainsMyData( true );
                    break;
                }
            }
        }

    }

    /**
     * Adjust the settings based on how many genes there are?
     * 
     * @param searchOptions
     * @param queryGeneIds
     */
    private void restrictSearchOptionsQueryGenes( CoexpressionSearchCommand searchOptions ) {

        /*
         * TODO: if there is only one experiment set, do a 'my genes only' query.
         */
        //
        // if ( searchOptions.getGeneIds().size() > MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY ) {
        // // this will be a 'my genes only' vis query since queryGeneIds !=null
        // searchOptions.setGeneIds( trimGeneIds( searchOptions.getGeneIds(), MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY ) );
        // } else if ( searchOptions.getQueryGenesOnly() ) {
        // // this will be the case where the user selects over 20 genes
        // searchOptions
        // .setGeneIds( trimGeneIds( searchOptions.getGeneIds(), MAX_GENES_PER_MY_GENES_ONLY_LARGE_QUERY ) );
        // } else {
        // searchOptions.setGeneIds( trimGeneIds( searchOptions.getGeneIds(), MAX_GENES_PER_QUERY ) );
        // }
    }

    /**
     * @param geneIds
     * @param limit
     * @return
     */
    private List<Long> trimGeneIds( Collection<Long> geneIds, int limit ) {
        return new ArrayList<Long>( geneIds ).subList( 0, Math.min( geneIds.size(), limit ) );
    }

}