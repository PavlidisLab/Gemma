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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.core.analysis.expression.coexpression.CoexpressionSearchCommand;
import ubic.gemma.core.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.core.analysis.expression.coexpression.GeneCoexpressionSearchService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.core.config.Settings;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author luke
 * @author paul
 */
@Controller
public class CoexpressionSearchController {

    private static final int DEFAULT_MAX_GENES_PER_MY_GENES_ONLY = 500;
    private static final int DEFAULT_MAX_RESULTS = 200;
    private static final int MAX_GENES_FOR_QUERY_GENES_ONLY_QUERY = Settings
            .getInt( "gemma.coexpressionSearch.maxGenesForQueryGenesOnly",
                    CoexpressionSearchController.DEFAULT_MAX_GENES_PER_MY_GENES_ONLY );
    private static final int MAX_RESULTS_PER_GENE = Settings.getInt( "gemma.coexpressionSearch.maxResultsPerQueryGene",
            CoexpressionSearchController.DEFAULT_MAX_RESULTS );

    private static final String NOTHING_FOUND_MESSAGE = "No coexpression found with those settings";
    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private GeneCoexpressionSearchService geneCoexpressionService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private TaskRunningService taskRunningService;

    /**
     * Used by CoexpressionSearchData.js - the main home page coexpression search.
     *
     * @param searchOptions search options
     * @return string
     */
    public String doBackgroundCoexSearch( CoexpressionSearchCommand searchOptions ) {
        CoexSearchTaskCommand options = new CoexSearchTaskCommand( searchOptions );
        CoexpressionSearchTask job = new CoexpressionSearchTask( options );
        return taskRunningService.submitTask( job );
    }

    /**
     * Important entry point - called by the CoexpressionSearchTask
     *
     * @param searchOptions search options
     * @return coexp. meta VO
     */
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {

        Collection<ExpressionExperiment> myEE = null;
        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();

        if ( searchOptions.getGeneIds() == null || searchOptions.getGeneIds().isEmpty() ) {

            if ( searchOptions.getGeneSetId() != null ) {
                searchOptions.setGeneIds(
                        geneSetService.getGeneIdsInGroup( new GeneSetValueObject( searchOptions.getGeneSetId() ) ) );
            }

            if ( searchOptions.getGeneIds().isEmpty() ) {
                result.setErrorState( "No genes were selected" );
                return result;
            }
        }

        if ( searchOptions.getGeneIds().size() > CoexpressionSearchController.MAX_GENES_FOR_QUERY_GENES_ONLY_QUERY ) {
            result.setErrorState( "Too many genes selected, please limit searches to "
                    + CoexpressionSearchController.MAX_GENES_FOR_QUERY_GENES_ONLY_QUERY + " genes" );
            return result;
        }

        if ( searchOptions.getGeneIds().size() == 0 ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;
        }

        // Add the user's datasets to the selected datasets
        if ( searchOptions.isUseMyDatasets() ) {
            myEE = expressionExperimentService.loadAll();
        }

        Collection<Long> eeIds = this.chooseExperimentsToQuery( searchOptions, result );

        if ( StringUtils.isNotBlank( result.getErrorState() ) )
            return result;

        if ( myEE != null && !myEE.isEmpty() ) {
            eeIds.addAll( IdentifiableUtils.getIds( myEE ) );
        } else {
            CoexpressionSearchController.log.info( "No user data to add" );
        }

        if ( eeIds.isEmpty() ) {
            // search all available experiments.
        } else {
            searchOptions.setEeIds( eeIds );
        }

        CoexpressionSearchController.log.info( "Starting coexpression search: " + searchOptions );

        result = geneCoexpressionService.coexpressionSearch( searchOptions.getEeIds(), searchOptions.getGeneIds(),
                searchOptions.getStringency(), CoexpressionSearchController.MAX_RESULTS_PER_GENE,
                searchOptions.getQueryGenesOnly() );

        // make sure to create an empty list instead of null for front-end
        if ( result.getResults() == null ) {
            List<CoexpressionValueObjectExt> res = new ArrayList<>();
            result.setResults( res );
        }

        if ( result.getResults().isEmpty() ) {
            result.setErrorState( CoexpressionSearchController.NOTHING_FOUND_MESSAGE );
            CoexpressionSearchController.log.info( "No search results for query: " + searchOptions );
        }

        return result;

    }

    /**
     * Do a search that fills in the edges among the genes already found. Maps to doSearchQuickComplete in javascript.
     *
     * @param searchOptions search options
     * @param queryGeneIds  the genes which were used originally to start the search
     * @return coexp meta VO
     */
    public CoexpressionMetaValueObject doSearchQuickComplete( CoexpressionSearchCommand searchOptions,
            Collection<Long> queryGeneIds ) {

        if ( searchOptions == null ) {
            throw new IllegalArgumentException( "Search options cannot be null" );
        }

        assert queryGeneIds != null && !queryGeneIds.isEmpty();

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        if ( queryGeneIds.size() == 1 ) {
            // there is nothing to do; really we shouldn't be here.
            assert !searchOptions.getGeneIds().isEmpty();
            return this.doSearch( searchOptions );
        }

        assert searchOptions.getQueryGenesOnly();

        // Add the user's datasets to the selected datasets
        Collection<ExpressionExperiment> myEE = null;
        if ( searchOptions.isUseMyDatasets() ) {
            myEE = expressionExperimentService.loadAll();
        }

        Collection<Long> eeIds = this.chooseExperimentsToQuery( searchOptions, result );

        if ( StringUtils.isNotBlank( result.getErrorState() ) )
            return result;

        if ( myEE != null && !myEE.isEmpty() ) {
            eeIds.addAll( IdentifiableUtils.getIds( myEE ) );
        } else {
            CoexpressionSearchController.log.debug( "No user data to add" );
        }

        if ( eeIds.isEmpty() ) {
            // search all available experiments.
        } else {
            searchOptions.setEeIds( eeIds );
        }

        StopWatch timer = new StopWatch();
        timer.start();

        CoexpressionSearchController.log
                .info( "Coexpression search for " + searchOptions.getGeneIds().size() + " genes, stringency="
                        + searchOptions.getStringency() + ( searchOptions.getEeIds() != null ?
                        ( " ees=" + searchOptions.getEeIds().size() ) :
                        " All ees" ) );

        result = geneCoexpressionService.coexpressionSearchQuick( searchOptions.getEeIds(), searchOptions.getGeneIds(),
                searchOptions.getStringency(), -1 /* no limit in this situation anyway */, true );

        result.setSearchSettings( searchOptions );
        // result.trim( new HashSet<>( queryGeneIds ) );

        if ( result.getResults() == null || result.getResults().isEmpty() ) {
            result.setErrorState( CoexpressionSearchController.NOTHING_FOUND_MESSAGE );
            CoexpressionSearchController.log.info( "No search results for query: " + searchOptions );
        }

        if ( timer.getTime() > 2000 ) {
            CoexpressionSearchController.log.info( "Search complete: " + result.getResults().size() + " hits" );
        }

        return result;
    }

    /**
     * Convert the search options to a set of experiment IDs (security-filtered)
     *
     * @param searchOptions search options
     * @param result        only used to set error state
     * @return ids
     */
    private Collection<Long> chooseExperimentsToQuery( CoexpressionSearchCommand searchOptions,
            CoexpressionMetaValueObject result ) {

        Long eeSetId = null;
        if ( searchOptions.getEeIds() != null && !searchOptions.getEeIds().isEmpty() ) {
            // security filter.
            return IdentifiableUtils.getIds( expressionExperimentService.load( searchOptions.getEeIds() ) );
        }

        if ( searchOptions.getEeSetId() != null ) {
            eeSetId = searchOptions.getEeSetId();
        } else if ( StringUtils.isNotBlank( searchOptions.getEeSetName() ) ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService
                    .findByName( searchOptions.getEeSetName() );
            if ( eeSets.size() == 1 ) {
                eeSetId = eeSets.iterator().next().getId();
            } else {
                result.setErrorState( "Unknown or ambiguous set name: " + searchOptions.getEeSetName() );
                return new HashSet<>();
            }
        }

        if ( eeSetId == null )
            return new HashSet<>();

        // security filter
        return IdentifiableUtils.getIds( expressionExperimentSetService.getExperimentsInSet( eeSetId ) );

    }

    //
    // /**
    // * @param vo
    // * @param eesToFlag
    // */
    // private void addMyDataFlag( CoexpressionMetaValueObject vo, Collection<ExpressionExperiment> eesToFlag ) {
    //
    // Collection<Long> eesToFlagIds = new ArrayList<Long>();
    // for ( ExpressionExperiment ee : eesToFlag ) {
    // eesToFlagIds.add( ee.getId() );
    // }
    //
    // if ( vo == null || vo.getResults() == null || vo.getResults().isEmpty() ) return;
    //
    // for ( CoexpressionValueObjectExt covo : vo.getResults() ) {
    // for ( Long eeToFlag : eesToFlagIds ) {
    // if ( covo.getSupportingExperiments().contains( eeToFlag ) ) {
    // covo.setContainsMyData( true );
    // break;
    // }
    // }
    // }
    //
    // }

    /**
     * Inner class used for doing a long running coex search
     */
    class CoexpressionSearchTask extends AbstractTask<TaskCommand> {

        public CoexpressionSearchTask( CoexSearchTaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

            CoexSearchTaskCommand coexCommand = ( CoexSearchTaskCommand ) taskCommand;

            CoexpressionMetaValueObject results = CoexpressionSearchController.this
                    .doSearch( coexCommand.getSearchOptions() );

            return new TaskResult( taskCommand, results );

        }
    }

}