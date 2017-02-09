/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.tasks.visualization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.ContrastVO;
import ubic.gemma.model.analysis.expression.diff.ContrastsValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExResultSetSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.MissingResult;
import ubic.gemma.model.analysis.expression.diff.NonRetainedResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.tasks.AbstractTask;
import ubic.gemma.tasks.visualization.DifferentialExpressionGenesConditionsValueObject.Condition;
import ubic.gemma.util.EntityUtils;

/**
 * Encapsulates the search for differential expression results, for a set of genes and experiments (which can be
 * grouped)
 * 
 * @author anton
 * @version $Id$
 */
@Component
@Scope("prototype")
public class DifferentialExpressionSearchTaskImpl extends
        AbstractTask<TaskResult, DifferentialExpressionSearchTaskCommand> implements DifferentialExpressionSearchTask {

    protected static Log log = LogFactory.getLog( DifferentialExpressionSearchTaskImpl.class );

    private static final Double PVALUE_CONTRAST_SELECT_THRESHOLD = 0.05;

    /**
     * Pvalues smaller than this (e.g., 0 are set to this value instead.
     */
    private static final double TINY_PVALUE = 1e-16;

    private static final double TINY_QVALUE = 1e-10;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    private String experimentGroupName;

    private Collection<ExpressionExperimentValueObject> experimentGroup;

    @Autowired
    private ExpressionExperimentSubSetService experimentSubSetService;

    private String geneGroupName;

    private Collection<GeneValueObject> geneGroup;

    /*
     * Does all the actual work of the query. (non-Javadoc)
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public TaskResult execute() {

        log.info( "==== Starting diff ex search ==== " + this.taskCommand );
        DifferentialExpressionGenesConditionsValueObject searchResult = new DifferentialExpressionGenesConditionsValueObject();

        addGenesToSearchResultValueObject( searchResult );

        List<DiffExResultSetSummaryValueObject> resultSets = addConditionsToSearchResultValueObject( searchResult );

        fetchDifferentialExpressionResults( resultSets, getGeneIds( searchResult.getGenes() ), searchResult );

        log.info( "=== Finished DiffExpSearchTask: " + searchResult.getCellData().size() + " 'conditions' ..." );

        return new TaskResult( this.taskCommand, searchResult );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.tasks.AbstractTask#setTaskCommand(ubic.gemma.job.TaskCommand)
     */
    @Override
    public void setTaskCommand( DifferentialExpressionSearchTaskCommand taskCommand ) {
        super.setTaskCommand( taskCommand );

        this.geneGroup = taskCommand.getGeneGroup();
        this.experimentGroup = taskCommand.getExperimentGroup();
        this.geneGroupName = taskCommand.getGeneGroupName();
        this.experimentGroupName = taskCommand.getExperimentGroupName();
    }

    /**
     * Get information on the conditions to be searched. This is not part of the query for the results themselves, but
     * uses the database to get metadata/summaries about the analyses that will be used. Initializes the searchResult
     * value object. Later, values which are non-missing will be replaced with either 'non-significant' or 'significant'
     * results.
     * 
     * @param searchResult to be initialized
     * @return lsit of the resultSets that should be queried.
     */
    private List<DiffExResultSetSummaryValueObject> addConditionsToSearchResultValueObject(
            DifferentialExpressionGenesConditionsValueObject searchResult ) {

        StopWatch watch = new StopWatch( "addConditionsToSearchResultValueObject" );
        watch.start( "Add conditions to search result value object" );
        List<DiffExResultSetSummaryValueObject> usedResultSets = new LinkedList<>();

        int i = 0;

        log.info( "Loading " + experimentGroupName + " experiments..." );

        // database hit: important that this be fast.
        Map<ExpressionExperimentValueObject, Collection<DifferentialExpressionAnalysisValueObject>> analyses = differentialExpressionAnalysisService
                .getAnalysesByExperiment( EntityUtils.getIds( experimentGroup ) );

        experiment: for ( ExpressionExperimentValueObject bas : analyses.keySet() ) {

            Collection<DifferentialExpressionAnalysisValueObject> analysesForExperiment = filterAnalyses(
                    analyses.get( bas ) );

            if ( analysesForExperiment.isEmpty() ) {
                continue;
            }

            /*
             * There will often just be one analysis for the experiment. Exception would be when there is subsetting.
             */
            for ( DifferentialExpressionAnalysisValueObject analysis : analysesForExperiment ) {

                List<DiffExResultSetSummaryValueObject> resultSets = filterResultSets( analysis );
                usedResultSets.addAll( resultSets );

                if ( resultSets.isEmpty() ) {
                    log.info( "No resultSets usable for " + bas.getId() );
                }

                for ( DiffExResultSetSummaryValueObject resultSet : resultSets ) {

                    // this is taken care of by the filterResultSets
                    assert resultSet.getNumberOfDiffExpressedProbes() != null; // sanity check.
                    assert resultSet.getExperimentalFactors().size() == 1; // interactions not okay

                    ExperimentalFactorValueObject factor = resultSet.getExperimentalFactors().iterator().next();

                    Collection<FactorValueValueObject> factorValues = filterFactorValues( analysis, factor.getValues(),
                            resultSet.getBaselineGroup().getId() );

                    if ( factorValues.isEmpty() ) {
                        /*
                         * This can only happen if there is just a baseline factorvalue. Even for one-sided tests //
                         * that // won't be the case.
                         */
                        log.warn( "Nothing usable for resultSet=" + resultSet.getResultSetId() );
                        continue;
                    }

                    for ( FactorValueValueObject factorValue : factorValues ) {

                        Condition condition = searchResult.new Condition( bas, analysis, resultSet, factorValue );

                        condition.setExperimentGroupName( experimentGroupName );

                        /*
                         * SANITY CHECKS these fields should be filled in. If not, we are going to skip the results.
                         */
                        if ( condition.getNumberDiffExpressedProbes() == -1 ) {
                            log.warn( bas + ": Error: No hit list sizes for resultSet with ID="
                                    + resultSet.getResultSetId() );
                            continue;
                        }
                        if ( condition.getNumberOfProbesOnArray() == null
                                || condition.getNumberDiffExpressedProbes() == null ) {
                            log.error(
                                    bas + ": Error: Null counts for # diff ex probe or # probes on array, Skipping" );
                            continue experiment;
                        } else if ( condition.getNumberOfProbesOnArray() < condition.getNumberDiffExpressedProbes() ) {
                            log.error( bas + ": Error: More diff expressed probes than probes on array. Skipping." );
                            continue experiment;
                        }

                        searchResult.addCondition( condition );

                        i++;
                    }
                }
            }

        }

        watch.stop();
        if ( watch.getTotalTimeMillis() > 100 ) {
            // This does not include getting the actual diff ex results.
            log.info( "Get information on conditions/analyses for " + i + " factorValues: " + watch.getTotalTimeMillis()
                    + "ms" );
        }

        return usedResultSets;
    }

    /**
     * No database calls here, just organization.
     * 
     * @param searchResult
     */
    private void addGenesToSearchResultValueObject( DifferentialExpressionGenesConditionsValueObject searchResult ) {

        log.info( "Loading genes for " + geneGroupName + " ..." );
        for ( GeneValueObject gene : geneGroup ) {
            DifferentialExpressionGenesConditionsValueObject.DiffExGene g = searchResult.new DiffExGene( gene.getId(),
                    gene.getOfficialSymbol(), gene.getOfficialName() );
            g.setGroupName( geneGroupName );
            searchResult.addGene( g );
        }

    }

    /**
     * Gets all the diff ex results, flattening out the relation with resultset and gene (the objs still have this
     * information in them)
     * 
     * @param resultSetToGeneResults
     * @return
     */
    private Collection<DiffExprGeneSearchResult> aggregateAcrossResultSets(
            Map<Long, Map<Long, DiffExprGeneSearchResult>> resultSetToGeneResults ) {
        Collection<DiffExprGeneSearchResult> aggregatedResults = new HashSet<>();

        for ( Entry<Long, Map<Long, DiffExprGeneSearchResult>> resultSetEntry : resultSetToGeneResults.entrySet() ) {
            Collection<DiffExprGeneSearchResult> values = resultSetEntry.getValue().values();
            aggregatedResults.addAll( values );
        }

        return aggregatedResults;
    }

    /**
     * Main processing: fetch diff ex results.
     * 
     * @param resultSets to be searched
     * @param geneIds to be searched
     * @param searchResult holds the results
     */
    private void fetchDifferentialExpressionResults( List<DiffExResultSetSummaryValueObject> resultSets,
            List<Long> geneIds, DifferentialExpressionGenesConditionsValueObject searchResult ) {

        StopWatch watch = new StopWatch( "Process differential expression search" );
        watch.start( "Fetch diff ex results" );

        // Main query for results; the main time sink.
        Map<Long, Map<Long, DiffExprGeneSearchResult>> resultSetToGeneResults = differentialExpressionResultService
                .findDiffExAnalysisResultIdsInResultSets( resultSets, geneIds );
        watch.stop();

        Collection<DiffExprGeneSearchResult> aggregatedResults = aggregateAcrossResultSets( resultSetToGeneResults );

        watch.start( "Fetch details for contrasts for " + aggregatedResults.size() + " results" );
        Map<Long, ContrastsValueObject> detailedResults = getDetailsForContrasts( aggregatedResults );

        processHits( searchResult, resultSetToGeneResults, resultSets, detailedResults );

        watch.stop();
        log.info( "Diff ex search finished:\n" + watch.prettyPrint() );
    }

    /**
     * If there are multiple analyses for an experiment, pick the one(s) that "don't overlap" (see implementation for
     * details, evolving). No database hits.
     * 
     * @param analyses, all from a single experiment
     * @return a collection with either 0 or a small number of non-conflicting analyses.
     */
    private Collection<DifferentialExpressionAnalysisValueObject> filterAnalyses(
            Collection<DifferentialExpressionAnalysisValueObject> analyses ) {

        // easy case.
        if ( analyses.size() == 1 ) return analyses;

        Collection<DifferentialExpressionAnalysisValueObject> filtered = new HashSet<>();

        Long subsetFactor = null;
        Map<DifferentialExpressionAnalysisValueObject, Collection<ExperimentalFactorValueObject>> analysisFactorsUsed = new HashMap<>();
        for ( DifferentialExpressionAnalysisValueObject analysis : analyses ) {

            /*
             * If the experiment has more than one subsetted analysis, we only used one.
             */
            if ( analysis.isSubset() ) {
                if ( subsetFactor == null || analysis.getSubsetFactorValue().getId().equals( subsetFactor ) ) {
                    filtered.add( analysis );
                    log.info( "Selecting subsetanalysis: " + analysis + " " + analysis.getSubsetFactorValue() );
                } else {
                    log.info( "Skipping: subsetanalysis" + analysis + " " + analysis.getSubsetFactorValue() );
                }

                subsetFactor = analysis.getSubsetFactorValue().getFactorId();

            } else {

                List<DiffExResultSetSummaryValueObject> resultSets = filterResultSets( analysis );
                Collection<ExperimentalFactorValueObject> factorsUsed = new HashSet<>();

                for ( DiffExResultSetSummaryValueObject rs : resultSets ) {
                    if ( isBatch( rs ) ) continue;
                    Collection<ExperimentalFactorValueObject> facts = rs.getExperimentalFactors();
                    for ( ExperimentalFactorValueObject f : facts ) {
                        if ( ExperimentalDesignUtils.isBatch( f ) ) continue;
                        factorsUsed.add( f );
                    }
                }
                if ( factorsUsed.isEmpty() ) continue;
                analysisFactorsUsed.put( analysis, factorsUsed );
            }
        }

        /*
         * If we got a subsetted group of analyses, just use them
         */
        if ( !filtered.isEmpty() ) {
            log.info( "Using subsetted analyses for " + analyses.iterator().next().getBioAssaySetId() + "("
                    + analyses.size() + " analyses)" );
            return filtered;
        }

        if ( analysisFactorsUsed.isEmpty() ) {
            assert filtered.isEmpty();
            log.info( "No analyses were usable for " + analyses.iterator().next().getBioAssaySetId() );
            return filtered;
        }

        /*
         * Look for the analysis that has the most factors. We might change this to pick more than one if they use
         * different factors, but this would be pretty rare.
         */
        assert !analysisFactorsUsed.isEmpty();
        DifferentialExpressionAnalysisValueObject best = null;
        for ( DifferentialExpressionAnalysisValueObject candidate : analysisFactorsUsed.keySet() ) {
            if ( best == null
                    || analysisFactorsUsed.get( best ).size() < analysisFactorsUsed.get( candidate ).size() ) {
                best = candidate;
            }
        }
        if ( best != null ) {
            filtered.add( best );
            log.info( "Selecting :" + best );

        }

        return filtered;

    }

    /**
     * @param analysis
     * @param factorValues for a factor used for a particular resultset
     * @param baselineFactorValueId
     * @return
     */
    private List<FactorValueValueObject> filterFactorValues( DifferentialExpressionAnalysisValueObject analysis,
            Collection<FactorValueValueObject> factorValues, long baselineFactorValueId ) {
        List<FactorValueValueObject> filteredFactorValues = new LinkedList<>();

        Collection<FactorValueValueObject> keepForSubSet = maybeGetSubSetFactorValuesToKeep( analysis,
                factorValues.iterator().next().getFactorId() );

        for ( FactorValueValueObject factorValue : factorValues ) {
            if ( factorValue.getId().equals( baselineFactorValueId ) ) continue; // Skip baseline

            // skip fvs not used in the subset, if it is a subset
            if ( keepForSubSet != null && !keepForSubSet.contains( factorValue ) ) continue;

            filteredFactorValues.add( factorValue );
        }

        return filteredFactorValues;
    }

    /**
     * Remove resultSets that are not usable for one reason or another (e.g., intearctions, batch effects); no database
     * hits.
     * 
     * @param resultSets
     * @return
     */
    private List<DiffExResultSetSummaryValueObject> filterResultSets(
            DifferentialExpressionAnalysisValueObject analysis ) {
        List<DiffExResultSetSummaryValueObject> filteredResultSets = new LinkedList<>();

        for ( DiffExResultSetSummaryValueObject resultSet : analysis.getResultSets() ) {

            // Skip interactions.
            if ( resultSet.getExperimentalFactors().size() != 1 ) continue;

            // Skip batch effect ones.
            if ( isBatch( resultSet ) ) continue;

            // Skip if baseline is not specified.
            if ( resultSet.getBaselineGroup() == null ) {
                log.error( "Possible Data Issue: resultSet.getBaselineGroup() returned null for result set with ID="
                        + resultSet.getResultSetId() );
                continue;
            }

            // must have hitlists populated
            if ( resultSet.getNumberOfDiffExpressedProbes() == null ) {
                log.warn( "Possible data issue: resultSet.getHitListSizes() returned null for result set with ID="
                        + resultSet.getResultSetId() );
                continue;
            }

            filteredResultSets.add( resultSet );
        }

        return filteredResultSets;
    }

    /**
     * Retrieve the details (contrasts) for results which meet the criterion. (PVALUE_CONTRAST_SELECT_THRESHOLD).
     * Requires a database hit.
     * 
     * @param geneToProbeResult
     * @return
     */
    private Map<Long, ContrastsValueObject> getDetailsForContrasts(
            Collection<DiffExprGeneSearchResult> diffExResults ) {

        StopWatch timer = new StopWatch();
        timer.start();
        List<Long> resultsWithContrasts = new ArrayList<>();

        for ( DiffExprGeneSearchResult r : diffExResults ) {
            if ( r.getResultId() == null ) {
                // it is a dummy result. It means there is no result for this gene in this resultset.
                continue;
            }

            /*
             * this check will not be needed if we only store the 'good' results, but we do store everything.
             */
            // Here I am trying to avoid fetching them when there is no hope that the results will be interesting.
            if ( r instanceof MissingResult || r instanceof NonRetainedResult
                    || r.getCorrectedPvalue() > PVALUE_CONTRAST_SELECT_THRESHOLD ) {
                // Then it won't have contrasts; no need to fetch.
                continue;
            }

            resultsWithContrasts.add( r.getResultId() );
        }

        Map<Long, ContrastsValueObject> detailedResults = new HashMap<>();
        if ( !resultsWithContrasts.isEmpty() ) {
            // uses a left join so it will have all the results.
            detailedResults = differentialExpressionResultService.loadContrastDetailsForResults( resultsWithContrasts );
        }

        timer.stop();
        if ( timer.getTotalTimeMillis() > 1 ) {
            log.info( "Fetch contrasts for " + resultsWithContrasts.size() + " results: " + timer.getTotalTimeMillis()
                    + "ms" );
        }
        return detailedResults;
    }

    /**
     * @param g
     * @return
     */
    private List<Long> getGeneIds( Collection<DifferentialExpressionGenesConditionsValueObject.DiffExGene> g ) {
        List<Long> ids = new LinkedList<Long>();
        for ( DifferentialExpressionGenesConditionsValueObject.DiffExGene gene : g ) {
            ids.add( gene.getId() );
        }
        return ids;
    }

    /**
     * @param resultSet
     * @return
     */
    private boolean isBatch( DiffExResultSetSummaryValueObject resultSet ) {
        for ( ExperimentalFactorValueObject factor : resultSet.getExperimentalFactors() ) {
            if ( ExperimentalDesignUtils.isBatch( factor ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param resultSet
     * @param geneId
     * @param searchResult
     * @param correctedPvalue should not be null.
     * @param pValue should not be null.
     * @param numProbes
     * @param numProbesDiffExpressed
     */
    private void markCellsBlack( DiffExResultSetSummaryValueObject resultSet, Long geneId,
            DifferentialExpressionGenesConditionsValueObject searchResult, Double correctedPvalue, Double pValue,
            int numProbes, int numProbesDiffExpressed ) {

        /*
         * Note that if the resultSet has more than one experimental factor, it is an interaction term.
         */
        assert resultSet.getExperimentalFactors().size() == 1 : "Should not have been passed an interaction term";

        ExperimentalFactorValueObject experimentalFactor = resultSet.getExperimentalFactors().iterator().next();
        Collection<FactorValueValueObject> factorValues = experimentalFactor.getValues();
        for ( FactorValueValueObject factorValue : factorValues ) {
            String conditionId = DifferentialExpressionGenesConditionsValueObject
                    .constructConditionId( resultSet.getResultSetId(), factorValue.getId() );
            searchResult.addBlackCell( geneId, conditionId, correctedPvalue, pValue, numProbes,
                    numProbesDiffExpressed );
        }
    }

    /**
     * we have to skip factorvalues that are not part of the subset -- if it is a subset. Hits the database.
     * 
     * @param analysis
     * @param experimentalFactor
     * @return
     */
    private Collection<FactorValueValueObject> maybeGetSubSetFactorValuesToKeep(
            DifferentialExpressionAnalysisValueObject analysis, Long experimentalFactor ) {
        Collection<FactorValueValueObject> keepForSubSet = null;
        if ( analysis.isSubset() ) {

            Long eeid = analysis.getBioAssaySetId();
            keepForSubSet = this.experimentSubSetService.getFactorValuesUsed( eeid, experimentalFactor );
            // could this be empty?
            if ( keepForSubSet.isEmpty() ) {
                log.warn( "No factorvalues were usable for " + experimentalFactor + " from " + analysis );
            }
        }
        return keepForSubSet;
    }

    /**
     * @param searchResult
     * @param resultSetToGeneResults
     * @param resultSetMap
     * @param detailedResults
     */
    private void processHits( DifferentialExpressionGenesConditionsValueObject searchResult,
            Map<Long, Map<Long, DiffExprGeneSearchResult>> resultSetToGeneResults,
            Collection<DiffExResultSetSummaryValueObject> resultSets,
            Map<Long, ContrastsValueObject> detailedResults ) {

        int i = 0;
        Map<Long, DiffExResultSetSummaryValueObject> resultSetMap = EntityUtils.getIdMap( resultSets,
                "getResultSetId" );

        for ( Entry<Long, Map<Long, DiffExprGeneSearchResult>> resultSetEntry : resultSetToGeneResults.entrySet() ) {

            Map<Long, DiffExprGeneSearchResult> geneToProbeResult = resultSetEntry.getValue();

            DiffExResultSetSummaryValueObject resultSet = resultSetMap.get( resultSetEntry.getKey() );
            assert resultSet != null;

            processHitsForResultSet( searchResult, detailedResults, geneToProbeResult, resultSet );

            if ( ++i % 2000 == 0 ) {
                log.info( "Processed " + i + "/" + resultSetToGeneResults.size() + " hits." );
            }
        }

    }

    /**
     * @param searchResult
     * @param detailedResults
     * @param geneToProbeResult
     * @param resultSet
     */
    private void processHitsForResultSet( DifferentialExpressionGenesConditionsValueObject searchResult,
            Map<Long, ContrastsValueObject> detailedResults, Map<Long, DiffExprGeneSearchResult> geneToProbeResult,
            DiffExResultSetSummaryValueObject resultSet ) {

        // No database calls.
        if ( log.isDebugEnabled() ) log.debug( "Start processing hits for result sets." );
        try {
            boolean warned = false; // avoid too many warnings ...
            for ( Long geneId : geneToProbeResult.keySet() ) {
                DiffExprGeneSearchResult diffExprGeneSearchResult = geneToProbeResult.get( geneId );

                if ( diffExprGeneSearchResult instanceof MissingResult ) {
                    continue;
                }

                if ( diffExprGeneSearchResult instanceof NonRetainedResult ) {
                    /*
                     * mark the cell as non-significant, otherwise it will just be left as missing. Add values for all
                     * the contrasts
                     */
                    searchResult.setAsNonSignficant( geneId, diffExprGeneSearchResult.getResultSetId() );
                    continue;
                }

                Double correctedPvalue = diffExprGeneSearchResult.getCorrectedPvalue();
                Double uncorrectedPvalue = diffExprGeneSearchResult.getPvalue();

                assert uncorrectedPvalue != null;

                // arbitrary fixing (meant to deal with zeros). Remember these are usually FDRs.
                if ( correctedPvalue < TINY_QVALUE ) {
                    correctedPvalue = TINY_QVALUE;
                }

                if ( uncorrectedPvalue < TINY_PVALUE ) {
                    uncorrectedPvalue = TINY_PVALUE;
                }

                int numberOfProbes = diffExprGeneSearchResult.getNumberOfProbes();
                int numberOfProbesDiffExpressed = diffExprGeneSearchResult.getNumberOfProbesDiffExpressed();

                markCellsBlack( resultSet, geneId, searchResult, correctedPvalue, uncorrectedPvalue, numberOfProbes,
                        numberOfProbesDiffExpressed );

                Long probeResultId = diffExprGeneSearchResult.getResultId();
                if ( !detailedResults.containsKey( probeResultId ) ) {
                    continue;
                }

                ContrastsValueObject deaResult = detailedResults.get( probeResultId );

                for ( ContrastVO cr : deaResult.getContrasts() ) {
                    Long factorValue = cr.getFactorValueId();
                    if ( factorValue == null ) {
                        if ( !warned ) {
                            log.error( "Data Integrity error: Null factor value for contrast with id=" + cr.getId()
                                    + " associated with diffexresult " + deaResult.getResultId() + " for resultset "
                                    + resultSet.getResultSetId()
                                    + ". (additional warnings may be suppressed but additional results will be omitted)" );
                            warned = true;
                        }
                        continue;
                    }

                    String conditionId = DifferentialExpressionGenesConditionsValueObject
                            .constructConditionId( resultSet.getResultSetId(), factorValue );

                    if ( cr.getLogFoldChange() == null && !warned ) {
                        log.warn( "Fold change was null for contrast " + cr.getId() + " associated with diffexresult "
                                + deaResult.getResultId() + " for resultset " + resultSet.getResultSetId()
                                + ". (additional warnings may be suppressed)" );
                        warned = true;
                    }

                    searchResult.addCell( geneId, conditionId, correctedPvalue, cr.getLogFoldChange(), numberOfProbes,
                            numberOfProbesDiffExpressed, uncorrectedPvalue );
                }

            }
        } catch ( Exception e ) {
            log.error( e.getMessage(), e );
        }
        if ( log.isDebugEnabled() ) log.debug( "Done processing hits for result sets." );
    }
}
