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
package ubic.gemma.web.visualization;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject.Condition;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TODO Document Me
 * 
 * @author anton
 * @version $Id$
 */
@Component
public class DifferentialExpressionGeneConditionSearchServiceImpl implements
        DifferentialExpressionGeneConditionSearchService {

    private static final Double PVALUE_CONTRAST_SELECT_THRESHOLD = 0.05;

    public static class TaskProgress {
        private double progressPercent;
        private String currentStage;

        private DifferentialExpressionGenesConditionsValueObject taskResult = null;

        public TaskProgress( String stage, double percent, DifferentialExpressionGenesConditionsValueObject result ) {
            this.currentStage = stage;
            this.progressPercent = percent;
            this.taskResult = result;
        }

        public String getCurrentStage() {
            return this.currentStage;
        }

        public double getProgressPercent() {
            return this.progressPercent;
        }

        public DifferentialExpressionGenesConditionsValueObject getTaskResult() {
            return taskResult;
        }

        public void setTaskResult( DifferentialExpressionGenesConditionsValueObject taskResult ) {
            this.taskResult = taskResult;
        }
    }

    /**
     * Encapulsates the search for differential expression results, for a set of genes and experiments (which can be
     * grouped)
     */
    private class DifferentialExpressionSearchTask implements
            Callable<DifferentialExpressionGenesConditionsValueObject> {

        /**
         * Pvalues smaller than this (e.g., 0 are set to this value instead.
         */
        private static final double TINY_PVALUE = 1e-16;

        private static final double TINY_QVALUE = 1e-10;

        private List<List<Gene>> geneGroups;

        private List<Collection<ExpressionExperiment>> experimentGroups;

        private List<String> geneGroupNames;

        private List<String> experimentGroupNames;

        private String taskProgressStage = "Query submitted...";
        private double taskProgressPercent = 0.0;

        private DifferentialExpressionGenesConditionsValueObject taskResult = null;

        /**
         * @param geneGroups - the sets of genes to query
         * @param experimentGroups - the sets of experiments to query
         * @param geneGroupNames - metadata
         * @param experimentGroupNames
         */
        public DifferentialExpressionSearchTask( List<List<Gene>> geneGroups,
                List<Collection<ExpressionExperiment>> experimentGroups, List<String> geneGroupNames,
                List<String> experimentGroupNames ) {

            assert !geneGroups.isEmpty() && !geneGroups.get( 0 ).isEmpty();
            assert !experimentGroups.isEmpty() && !experimentGroups.get( 0 ).isEmpty();
            assert geneGroups.size() == geneGroupNames.size();
            assert experimentGroups.size() == experimentGroupNames.size();

            this.geneGroups = geneGroups;
            this.experimentGroups = experimentGroups;
            this.geneGroupNames = geneGroupNames;
            this.experimentGroupNames = experimentGroupNames;

        }

        /*
         * Does all the actual work of the query. (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public DifferentialExpressionGenesConditionsValueObject call() {
            log.info( "Call..." );
            DifferentialExpressionGenesConditionsValueObject searchResult = new DifferentialExpressionGenesConditionsValueObject();

            addGenesToSearchResultValueObject( searchResult );

            List<ExpressionAnalysisResultSet> resultSets = addConditionsToSearchResultValueObject( searchResult );

            fillHeatmapCells( resultSets, getGeneIds( searchResult.getGenes() ), searchResult );
            this.taskResult = searchResult;

            log.info( "Finished DiffExpSearchTask." );
            return searchResult;
        }

        /**
         * @return
         */
        public synchronized TaskProgress getTaskProgress() {
            // I think this is safe only because String is immutable
            // and double is copied by value. FIXME ????
            return new TaskProgress( this.taskProgressStage, this.taskProgressPercent, this.taskResult );
        }

        /**
         * Get information on the conditions to be searched. This is not part of the query for the results themselves,
         * but uses the database to get metadata/summaries about the analyses that will be used. Initializes the
         * searchResult value object.
         * 
         * @param searchResult to be initialized
         * @return lsit of the resultSets that should be queried.
         */
        private List<ExpressionAnalysisResultSet> addConditionsToSearchResultValueObject(
                DifferentialExpressionGenesConditionsValueObject searchResult ) {

            StopWatch watch = new StopWatch( "addConditionsToSearchResultValueObject" );
            watch.start( "Add conditions to search result value object" );
            List<ExpressionAnalysisResultSet> usedResultSets = new LinkedList<ExpressionAnalysisResultSet>();

            int experimentGroupIndex = 0;
            int i = 0;
            for ( Collection<ExpressionExperiment> experimentGroup : experimentGroups ) {

                String stage = "Loading " + experimentGroupNames.get( experimentGroupIndex ) + " experiments...";
                double progress = 0.0;
                double progressStep = 100.0 / experimentGroup.size();
                this.setTaskProgress( stage, progress );

                // database hit: important that this be fast.
                Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> analyses = differentialExpressionAnalysisService
                        .getAnalyses( experimentGroup );

                experiment: for ( BioAssaySet bas : analyses.keySet() ) {

                    if ( !( bas instanceof ExpressionExperiment ) ) {
                        log.warn( "Subsets not supported yet (" + bas + "), skipping" );
                        continue;
                    }

                    ExpressionExperiment experiment = ( ExpressionExperiment ) bas;

                    Collection<DifferentialExpressionAnalysis> analysesForExperiment = filterAnalyses( analyses
                            .get( experiment ) );

                    if ( analysesForExperiment.isEmpty() ) {
                        continue;
                    }

                    for ( DifferentialExpressionAnalysis analysis : analysesForExperiment ) {

                        List<ExpressionAnalysisResultSet> resultSets = filterResultSets( analysis.getResultSets() );
                        usedResultSets.addAll( resultSets );

                        if ( resultSets.isEmpty() ) {
                            log.info( "No resultSets usable for " + experiment.getShortName() );
                        }

                        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

                            // this is taken care of by the filterResultSets
                            assert resultSet.getHitListSizes() != null;
                            assert resultSet.getExperimentalFactors().size() == 1;

                            ExperimentalFactor factor = resultSet.getExperimentalFactors().iterator().next();
                            Collection<FactorValue> factorValues = filterFactorValues( factor.getFactorValues(),
                                    resultSet.getBaselineGroup().getId() );

                            for ( FactorValue factorValue : factorValues ) {

                                Condition condition = searchResult.new Condition( experiment, analysis, resultSet,
                                        factorValue );

                                condition.setExperimentGroupName( experimentGroupNames.get( experimentGroupIndex ) );
                                condition.setExperimentGroupIndex( experimentGroupIndex );

                                /*
                                 * SANITY CHECKS these fields should be filled in. If not, we are going to skip the
                                 * results.
                                 */
                                if ( condition.getNumberDiffExpressedProbes() == -1 ) {
                                    // Sorry, this is too slow and the hitlists should be filled in.
                                    // total = differentialExpressionAnalysisService.countProbesMeetingThreshold(
                                    // resultSet, 0.5 );
                                    log.warn( bas + ": Error: No hit list sizes for resultSet with ID="
                                            + resultSet.getId() );
                                    continue;
                                }
                                if ( condition.getNumberOfProbesOnArray() == null
                                        || condition.getNumberDiffExpressedProbes() == null ) {
                                    log.error( bas
                                            + ": Error: Null counts for # diff ex probe or # probes on array, Skipping" );
                                    continue experiment;
                                } else if ( condition.getNumberOfProbesOnArray() < condition
                                        .getNumberDiffExpressedProbes() ) {
                                    log.error( bas
                                            + ": Error: More diff expressed probes than probes on array. Skipping." );
                                    continue experiment;
                                }

                                searchResult.addCondition( condition );
                                i++;
                            }
                        }
                    }

                    progress += progressStep;
                    this.setTaskProgress( stage, progress );

                }
                experimentGroupIndex++;
            }

            watch.stop();
            if ( watch.getTotalTimeMillis() > 100 ) {
                // This does not include getting the actual diff ex results.
                log.info( "Get information on conditions/analyses for " + i + " items: " + watch.getTotalTimeMillis()
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
            int geneGroupIndex = 0;
            for ( List<Gene> geneGroup : geneGroups ) {
                String geneGroupName = geneGroupNames.get( geneGroupIndex );

                String stage = "Loading " + geneGroupName + " genes...";
                double progress = 0.0;
                double progressStep = 100 / geneGroup.size();
                this.setTaskProgress( stage, progress );
                for ( Gene gene : geneGroup ) {
                    DifferentialExpressionGenesConditionsValueObject.Gene g = searchResult.new Gene( gene.getId(),
                            gene.getOfficialSymbol(), gene.getOfficialName() );
                    g.setGroupIndex( geneGroupIndex );
                    g.setGroupName( geneGroupName );
                    searchResult.addGene( g );
                    progress += progressStep;
                    this.setTaskProgress( stage, progress );
                }
                geneGroupIndex++;
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
            int i = 0;
            Collection<DiffExprGeneSearchResult> aggregatedResults = new HashSet<DiffExprGeneSearchResult>();
            for ( Entry<Long, Map<Long, DiffExprGeneSearchResult>> resultSetEntry : resultSetToGeneResults.entrySet() ) {
                Collection<DiffExprGeneSearchResult> values = resultSetEntry.getValue().values();
                i += resultSetEntry.getValue().size();

                for ( DiffExprGeneSearchResult v : values ) {
                    if ( aggregatedResults.contains( v ) ) {
                        log.warn( "Already have : " + v );
                    }
                }

                aggregatedResults.addAll( values );
            }

            assert i == aggregatedResults.size();
            return aggregatedResults;
        }

        /**
         * The actual business of fetching the differential expression results.
         * 
         * @param resultSets
         * @param geneIds
         * @param searchResult holds the results
         */
        private void fillBatchOfHeatmapCells(
                Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed, List<Long> geneIds,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {

            StopWatch watch = new StopWatch( "Fill diff ex heatmap cells" );
            watch.start( "DB query for diff ex results" );

            // Main query for results.
            Map<Long, Map<Long, DiffExprGeneSearchResult>> resultSetToGeneResults = differentialExpressionResultService
                    .findDifferentialExpressionAnalysisResultIdsInResultSet( resultSetIdsToArrayDesignsUsed, geneIds );
            watch.stop();

            Map<Long, ExpressionAnalysisResultSet> resultSetMap = EntityUtils.getIdMap( resultSetIdsToArrayDesignsUsed
                    .keySet() );

            Collection<DiffExprGeneSearchResult> aggregatedResults = aggregateAcrossResultSets( resultSetToGeneResults );
            watch.start( "Processing " + aggregatedResults.size() + " results from DB query" );

            Map<Long, DifferentialExpressionAnalysisResult> detailedResults = getDetailsForContrasts( aggregatedResults );

            processHits( searchResult, resultSetToGeneResults, resultSetMap, detailedResults );

            watch.stop();
            log.info( "Diff ex search finished:\n" + watch.prettyPrint() );
        }

        /**
         * Staging for getting the diff ex results.
         * 
         * @param resultSets to be searched
         * @param geneIds to be searched
         * @param searchResult holds the results
         */
        private void fillHeatmapCells( List<ExpressionAnalysisResultSet> resultSets, List<Long> geneIds,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {
            this.setTaskProgress( "Starting analysis ...", this.getTaskProgress().getProgressPercent() + 10 );

            Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed = new HashMap<ExpressionAnalysisResultSet, Collection<Long>>();

            StopWatch timer = new StopWatch();
            timer.start();
            // DATABASE CALL HERE, but should be quite fast.
            for ( ExpressionAnalysisResultSet rs : resultSets ) {
                resultSetIdsToArrayDesignsUsed
                        .put( rs, EntityUtils.getIds( eeService.getArrayDesignsUsed( rs.getAnalysis()
                                .getExperimentAnalyzed() ) ) );
            }
            timer.stop();
            if ( timer.getTotalTimeMillis() > 100 ) {
                log.info( "Fetch array designs used: " + timer.getTotalTimeMillis() + "ms" );
            }

            fillBatchOfHeatmapCells( resultSetIdsToArrayDesignsUsed, geneIds, searchResult );

        }

        /**
         * If there are multiple analyses, pick the ones that "don't overlap" (see implementation for details, evolving)
         * 
         * @param collection
         * @return a collection with either 0 or a small number of non-conflicting analyses.
         */
        private Collection<DifferentialExpressionAnalysis> filterAnalyses(
                Collection<DifferentialExpressionAnalysis> analyses ) {

            // easy case.
            if ( analyses.size() == 1 ) return analyses;

            Collection<DifferentialExpressionAnalysis> filtered = new HashSet<DifferentialExpressionAnalysis>();

            ExperimentalFactor subsetFactor = null;
            Map<DifferentialExpressionAnalysis, Collection<ExperimentalFactor>> analysisFactorsUsed = new HashMap<DifferentialExpressionAnalysis, Collection<ExperimentalFactor>>();
            for ( DifferentialExpressionAnalysis analysis : analyses ) {

                // take the first subsetted analysis we see.
                if ( analysis.getExperimentAnalyzed() instanceof ExpressionExperimentSubSet ) {
                    differentialExpressionAnalysisService.thaw( analysis ); // NOTE necessary, but possibly slows things
                                                                            // down

                    if ( subsetFactor != null
                            && subsetFactor.equals( analysis.getSubsetFactorValue().getExperimentalFactor() ) ) {
                        filtered.add( analysis );
                    } else {
                        filtered.add( analysis );
                    }
                    subsetFactor = analysis.getSubsetFactorValue().getExperimentalFactor();
                } else {

                    List<ExpressionAnalysisResultSet> resultSets = filterResultSets( analysis.getResultSets() );
                    Collection<ExperimentalFactor> factorsUsed = new HashSet<ExperimentalFactor>();
                    for ( ExpressionAnalysisResultSet rs : resultSets ) {
                        if ( isBatch( rs ) ) continue;
                        Collection<ExperimentalFactor> facts = rs.getExperimentalFactors();
                        for ( ExperimentalFactor f : facts ) {
                            if ( ExperimentalDesignUtils.isBatch( f ) ) continue;
                            factorsUsed.add( f );
                        }
                    }
                    if ( factorsUsed.isEmpty() ) continue;
                    analysisFactorsUsed.put( analysis, factorsUsed );
                }
            }

            /*
             * If we got a subset analysis, just use it.
             */
            if ( !filtered.isEmpty() ) {
                log.info( "Using subsetted analyses for " + analyses.iterator().next().getExperimentAnalyzed() );
                return filtered;
            }

            if ( analysisFactorsUsed.isEmpty() ) {
                log.info( "No analyses were usable for " + analyses.iterator().next().getExperimentAnalyzed() );
                return filtered;
            }

            /*
             * Look for the analysis that has the most factors. We might change this to pick more than one if they use
             * different factors, but this would be pretty rare.
             */
            assert !analysisFactorsUsed.isEmpty();
            DifferentialExpressionAnalysis best = null;
            for ( DifferentialExpressionAnalysis candidate : analysisFactorsUsed.keySet() ) {
                if ( best == null
                        || analysisFactorsUsed.get( best ).size() < analysisFactorsUsed.get( candidate ).size() ) {
                    best = candidate;
                }
            }

            return filtered;

        }

        /**
         * @param factorValues
         * @param baselineFactorValueId
         * @return
         */
        private List<FactorValue> filterFactorValues( Collection<FactorValue> factorValues, long baselineFactorValueId ) {
            List<FactorValue> filteredFactorValues = new LinkedList<FactorValue>();

            for ( FactorValue factorValue : factorValues ) {
                if ( factorValue.getId().equals( baselineFactorValueId ) ) continue; // Skip baseline

                filteredFactorValues.add( factorValue );
            }

            return filteredFactorValues;
        }

        /**
         * Remove resultSets that are not usable for one reason or another (e.g., intearctions, batch effects)
         * 
         * @param resultSets
         * @return
         */
        private List<ExpressionAnalysisResultSet> filterResultSets( Collection<ExpressionAnalysisResultSet> resultSets ) {
            List<ExpressionAnalysisResultSet> filteredResultSets = new LinkedList<ExpressionAnalysisResultSet>();

            for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

                // Skip interactions.
                if ( resultSet.getExperimentalFactors().size() != 1 ) continue;

                // Skip batch effect ones.
                if ( isBatch( resultSet ) ) continue;

                // Skip if baseline is not specified.
                if ( resultSet.getBaselineGroup() == null ) {
                    log.error( "Possible Data Issue: resultSet.getBaselineGroup() returned null for result set with ID="
                            + resultSet.getId() );
                    continue;
                }

                // must have hitlists populated
                if ( resultSet.getHitListSizes() == null ) {
                    log.warn( "Possible data issue: resultSet.getHitListSizes() returned null for result set with ID="
                            + resultSet.getId() );
                    continue;
                }

                filteredResultSets.add( resultSet );
            }

            return filteredResultSets;
        }

        /**
         * Retrieve the details (contrasts) for results which meet the criterion. (PVALUE_CONTRAST_SELECT_THRESHOLD)
         * 
         * @param geneToProbeResult
         * @return
         */
        private Map<Long, DifferentialExpressionAnalysisResult> getDetailsForContrasts(
                Collection<DiffExprGeneSearchResult> diffExResults ) {

            StopWatch timer = new StopWatch();
            timer.start();
            List<Long> resultsWithContrasts = new ArrayList<Long>();
            for ( DiffExprGeneSearchResult r : diffExResults ) {
                if ( r.getResultId() == null ) {
                    // it is a dummy result. It means there is no result for this gene in this resultset.
                    continue;
                }

                // Here I am trying to avoid fetching them when there is no hope that the results will be interesting.
                if ( r.getCorrectedPvalue() == null || r.getCorrectedPvalue() > PVALUE_CONTRAST_SELECT_THRESHOLD ) {
                    // Then it won't have contrasts; no need to fetch.
                    continue;
                }

                resultsWithContrasts.add( r.getResultId() );
            }

            Map<Long, DifferentialExpressionAnalysisResult> detailedResults = new HashMap<Long, DifferentialExpressionAnalysisResult>();
            if ( !resultsWithContrasts.isEmpty() ) {
                detailedResults = EntityUtils
                        .getIdMap( differentialExpressionResultService.loadEagerContrasts( resultsWithContrasts ) );
            }

            timer.stop();
            if ( timer.getTotalTimeMillis() > 1 ) {
                log.info( "Fetch contrasts for " + resultsWithContrasts.size() + " results: "
                        + timer.getTotalTimeMillis() + "ms" );
            }
            return detailedResults;
        }

        /**
         * @param g
         * @return
         */
        private List<Long> getGeneIds( Collection<DifferentialExpressionGenesConditionsValueObject.Gene> g ) {
            List<Long> ids = new LinkedList<Long>();
            for ( DifferentialExpressionGenesConditionsValueObject.Gene gene : g ) {
                ids.add( gene.getId() );
            }
            return ids;
        }

        /**
         * @param resultSet
         * @return
         */
        private boolean isBatch( ExpressionAnalysisResultSet resultSet ) {
            for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
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
        private void markCellsBlack( ExpressionAnalysisResultSet resultSet, Long geneId,
                DifferentialExpressionGenesConditionsValueObject searchResult, Double correctedPvalue, Double pValue,
                int numProbes, int numProbesDiffExpressed ) {

            /*
             * Note that if the resultSet has more than one experimental factor, it is an interaction term.
             */
            assert resultSet.getExperimentalFactors().size() == 1 : "Should not have been passed an interaction term";

            ExperimentalFactor experimentalFactor = resultSet.getExperimentalFactors().iterator().next();
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            for ( FactorValue factorValue : factorValues ) {
                String conditionId = DifferentialExpressionGenesConditionsValueObject.constructConditionId(
                        resultSet.getId(), factorValue.getId() );
                searchResult.addBlackCell( geneId, conditionId, correctedPvalue, pValue, numProbes,
                        numProbesDiffExpressed );
            }
        }

        /**
         * @param searchResult
         * @param resultSetToGeneResults
         * @param resultSetMap
         * @param detailedResults
         */
        private void processHits( DifferentialExpressionGenesConditionsValueObject searchResult,
                Map<Long, Map<Long, DiffExprGeneSearchResult>> resultSetToGeneResults,
                Map<Long, ExpressionAnalysisResultSet> resultSetMap,
                Map<Long, DifferentialExpressionAnalysisResult> detailedResults ) {

            int i = 0;

            for ( Entry<Long, Map<Long, DiffExprGeneSearchResult>> resultSetEntry : resultSetToGeneResults.entrySet() ) {

                Map<Long, DiffExprGeneSearchResult> geneToProbeResult = resultSetEntry.getValue();

                ExpressionAnalysisResultSet resultSet = resultSetMap.get( resultSetEntry.getKey() );
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
                Map<Long, DifferentialExpressionAnalysisResult> detailedResults,
                Map<Long, DiffExprGeneSearchResult> geneToProbeResult, ExpressionAnalysisResultSet resultSet ) {
            // No database calls.
            log.info( "Start processing hits for result sets." );
            try {
                for ( Long geneId : geneToProbeResult.keySet() ) {
                    DiffExprGeneSearchResult diffExprGeneSearchResult = geneToProbeResult.get( geneId );

                    Double correctedPvalue = diffExprGeneSearchResult.getCorrectedPvalue();
                    Double uncorrectedPvalue = diffExprGeneSearchResult.getPvalue();

                    // this means we got a 'dummy' value, indicating missing data.
                    if ( correctedPvalue == null ) continue;
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

                    markCellsBlack( resultSet, geneId, searchResult, correctedPvalue, uncorrectedPvalue,
                            numberOfProbes, numberOfProbesDiffExpressed );

                    Long probeResultId = diffExprGeneSearchResult.getResultId();
                    if ( !detailedResults.containsKey( probeResultId ) ) {
                        continue;
                    }

                    DifferentialExpressionAnalysisResult deaResult = detailedResults.get( probeResultId );

                    for ( ContrastResult cr : deaResult.getContrasts() ) {
                        FactorValue factorValue = cr.getFactorValue();
                        assert factorValue != null : "Null factor value for contrast with id=" + cr.getId();
                        if ( factorValue == null ) {
                            log.error( "Data Integrity: Null factor value for contrast with id=" + cr.getId() );
                            continue;
                        }
                        String conditionId = DifferentialExpressionGenesConditionsValueObject.constructConditionId(
                                resultSet.getId(), factorValue.getId() );
                        searchResult.addCell( geneId, conditionId, correctedPvalue, cr.getLogFoldChange(),
                                numberOfProbes, numberOfProbesDiffExpressed, uncorrectedPvalue );
                    }

                }
            } catch ( Exception e ) {
                log.error( e.toString() );
                e.printStackTrace();
            }
            log.info( "Done processing hits for result sets." );
        }

        /**
         * @param stage
         * @param percent
         */
        private synchronized void setTaskProgress( String stage, double percent ) {
            this.taskProgressStage = stage;
            this.taskProgressPercent = percent;
        }

    }

    protected static Log log = LogFactory.getLog( DifferentialExpressionGeneConditionSearchServiceImpl.class.getName() );

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private CacheManager cacheManager;

    private Cache diffExpSearchTasksCache;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchService#getDiffExpSearchTaskProgress(java
     * .lang.String)
     */
    @Override
    public TaskProgress getDiffExpSearchTaskProgress( String taskId ) {
        if ( this.diffExpSearchTasksCache.isKeyInCache( taskId ) ) {
            DifferentialExpressionSearchTask diffExpSearchTask = ( DifferentialExpressionSearchTask ) this.diffExpSearchTasksCache
                    .get( taskId ).getObjectValue();

            TaskProgress taskProgress = diffExpSearchTask.getTaskProgress();
            DifferentialExpressionGenesConditionsValueObject result = taskProgress.getTaskResult();
            if ( result != null ) {
                this.diffExpSearchTasksCache.remove( taskId );
            }
            return taskProgress;
        }
        return new TaskProgress( "Removed", 0.0, null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchService#scheduleDiffExpSearchTask(java.
     * util.List, java.util.List, java.util.List, java.util.List)
     */
    @Override
    public String scheduleDiffExpSearchTask( List<List<Gene>> genes,
            List<Collection<ExpressionExperiment>> experiments, List<String> geneGroupNames,
            List<String> experimentGroupNames ) {

        log.info( "Got request to schedule search involving " + genes.size() + " gene groups and " + experiments.size()
                + " experiments groups" );

        DifferentialExpressionSearchTask diffExpSearchTask = new DifferentialExpressionSearchTask( genes, experiments,
                geneGroupNames, experimentGroupNames );

        String taskId = UUID.randomUUID().toString();
        this.diffExpSearchTasksCache.put( new Element( taskId, diffExpSearchTask ) );

        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        Future<DifferentialExpressionGenesConditionsValueObject> submit = singleThreadExecutor
                .submit( diffExpSearchTask );

        singleThreadExecutor.shutdown();

        return taskId;
    }

    @PostConstruct
    protected void init() {
        this.diffExpSearchTasksCache = new Cache( "DifferentialExpressionVisSearchTasks", 300, false, false, 3600, 3600 );
        this.cacheManager.addCache( this.diffExpSearchTasksCache );
    }

}