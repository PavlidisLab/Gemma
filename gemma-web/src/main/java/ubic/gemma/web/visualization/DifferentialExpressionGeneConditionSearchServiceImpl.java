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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

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
import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDaoImpl.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject.Condition;

/**
 * TODO Document Me
 * 
 * @author anton
 * @version $Id$
 */
@Component
public class DifferentialExpressionGeneConditionSearchServiceImpl implements
        DifferentialExpressionGeneConditionSearchService {

    public static class TaskProgress {
        private double progressPercent;
        private String currentStage;

        public TaskProgress( String stage, double percent ) {
            this.currentStage = stage;
            this.progressPercent = percent;
        }

        public String getCurrentStage() {
            return this.currentStage;
        }

        public double getProgressPercent() {
            return this.progressPercent;
        }
    }

    private class DifferentialExpressionSearchTask implements
            Callable<DifferentialExpressionGenesConditionsValueObject> {
        // private Log log = LogFactory.getLog( DifferentialExpressionSearchTask.class.getName() );
        private List<List<Gene>> genes;
        private List<Collection<ExpressionExperiment>> experiments;
        private List<String> geneGroupNames;
        private List<String> experimentGroupNames;

        private String taskProgressStage = "Search query submitted...";
        private double taskProgressPercent = 0.0;

        public DifferentialExpressionSearchTask( List<List<Gene>> genes,
                List<Collection<ExpressionExperiment>> experiments, List<String> geneGroupNames,
                List<String> experimentGroupNames ) {

            this.genes = genes;
            this.experiments = experiments;
            this.geneGroupNames = geneGroupNames;
            this.experimentGroupNames = experimentGroupNames;

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public DifferentialExpressionGenesConditionsValueObject call() {
            StopWatch watch = new StopWatch( "createGenesConditionsValueObject" );
            watch.start();

            // Order of calls matters (for now). Can be made more robust if needed.
            DifferentialExpressionGenesConditionsValueObject searchResult = new DifferentialExpressionGenesConditionsValueObject();

            addGenesToSearchResultValueObject( searchResult );

            List<ExpressionAnalysisResultSet> resultSets = addConditionsToSearchResultValueObject( searchResult );

            fillHeatmapCells( resultSets, getGeneIds( searchResult.getGenes() ), searchResult );

            watch.stop();
            log.info( watch.prettyPrint() );
            return searchResult;
        }

        /**
         * @return
         */
        public synchronized TaskProgress getTaskProgress() {
            // I think this is safe only because String is immutable
            // and double is copied by value. FIXME ????
            return new TaskProgress( this.taskProgressStage, this.taskProgressPercent );
        }

        /**
         * Get information on the conditions to be searched. This is not part of the query for the results themselves.
         * 
         * @param searchResult
         * @return
         */
        private List<ExpressionAnalysisResultSet> addConditionsToSearchResultValueObject(
                DifferentialExpressionGenesConditionsValueObject searchResult ) {

            StopWatch watch = new StopWatch( "addConditionsToSearchResultValueObject" );
            watch.start( "add conditions to search result value object" );
            List<ExpressionAnalysisResultSet> usedResultSets = new LinkedList<ExpressionAnalysisResultSet>();

            int experimentGroupIndex = 0;
            for ( Collection<ExpressionExperiment> experimentGroup : experiments ) {

                String stage = "Loading " + experimentGroupNames.get( experimentGroupIndex ) + " experiments...";
                double progress = 0.0;
                double progressStep = 100.0 / experimentGroup.size();
                this.setTaskProgress( stage, progress );

                // important that this be fast.
                Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> analyses = differentialExpressionAnalysisService
                        .getAnalyses( experimentGroup );

                experiment: for ( BioAssaySet bas : analyses.keySet() ) {

                    if ( !( bas instanceof ExpressionExperiment ) ) {
                        log.warn( "Subsets not supported yet (" + bas + "), skipping" );
                        continue;
                    }

                    ExpressionExperiment experiment = ( ExpressionExperiment ) bas;

                    List<DifferentialExpressionAnalysis> filteredAnalyses = filterAnalyses( analyses.get( experiment ) );

                    for ( DifferentialExpressionAnalysis analysis : filteredAnalyses ) {

                        List<ExpressionAnalysisResultSet> resultSets = filterResultSets( analysis.getResultSets() );
                        usedResultSets.addAll( resultSets );

                        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

                            if ( resultSet.getHitListSizes() == null ) {
                                // We have some data that hasn't been updated to the newer model.
                                continue experiment;
                            }

                            ExperimentalFactor factor = filterFactors( resultSet.getExperimentalFactors() );
                            Collection<FactorValue> factorValues = filterFactorValues( factor.getFactorValues(),
                                    resultSet.getBaselineGroup().getId() );

                            for ( FactorValue factorValue : factorValues ) {

                                Condition condition = searchResult.new Condition();

                                // FIXME is this a magic string?
                                condition.id = "rs:" + resultSet.getId() + "fv:" + factorValue.getId();
                                condition.experimentGroupName = experimentGroupNames.get( experimentGroupIndex );
                                condition.experimentGroupIndex = experimentGroupIndex;
                                condition.numberOfProbesOnArray = resultSet.getNumberOfProbesTested();
                                condition.numberOfGenesTested = resultSet.getNumberOfGenesTested(); // FIXME USE THIS

                                condition.datasetShortName = experiment.getShortName();
                                condition.datasetName = experiment.getName();
                                condition.datasetId = experiment.getId();
                                condition.analysisId = analysis.getId();
                                condition.resultSetId = resultSet.getId();

                                int up = -1, down = -1, total = -1;
                                if ( log.isDebugEnabled() )
                                    log.debug( "Got " + resultSet.getHitListSizes().size()
                                            + " hit list sizes for result set=" + resultSet.getId() );

                                for ( HitListSize h : resultSet.getHitListSizes() ) {
                                    if ( h.getThresholdQvalue() == 0.01 ) {
                                        if ( h.getDirection().equals( Direction.DOWN ) ) {
                                            down = h.getNumberOfProbes();
                                        } else if ( h.getDirection().equals( Direction.UP ) ) {
                                            up = h.getNumberOfProbes();
                                        } else if ( h.getDirection().equals( Direction.EITHER ) ) {
                                            total = h.getNumberOfProbes();
                                        }
                                    }
                                }

                                if ( total == -1 ) {
                                    // FIXME this is not necessary if the HitLists are populated.
                                    total = differentialExpressionAnalysisService.countProbesMeetingThreshold(
                                            resultSet, 0.5 );
                                }

                                condition.numberDiffExpressedProbes = total;
                                condition.numberDiffExpressedProbesUp = up;
                                condition.numberDiffExpressedProbesDown = down;
                                condition.baselineFactorValueId = resultSet.getBaselineGroup().getId();
                                condition.baselineFactorValue = getFactorValueString( resultSet.getBaselineGroup() );
                                condition.factorName = factor.getName();
                                condition.contrastFactorValue = getFactorValueString( factorValue );
                                condition.factorDescription = factor.getDescription();
                                condition.factorId = factor.getId();
                                /* FIXME can we use 'None' instead of 'null'? Is this a magic string? */
                                condition.factorCategory = ( factor.getCategory() == null ) ? "null" : factor
                                        .getCategory().getCategory();

                                /*
                                 * SANITY CHECKS these fields should be filled in. If not, we are going to skip the
                                 * results.
                                 */
                                if ( condition.numberOfProbesOnArray == null
                                        || condition.numberDiffExpressedProbes == null ) {
                                    log.error( bas
                                            + ": Error:Null counts for # diff ex probe or # probes on array, Skipping" );
                                    continue experiment;
                                } else if ( condition.numberOfProbesOnArray < condition.numberDiffExpressedProbes ) {
                                    log.error( bas
                                            + ": Error: More diff expressed probes than probes on array. Skipping." );
                                    continue experiment;
                                }

                                searchResult.addCondition( condition );
                            }
                        }
                    }
                    progress += progressStep;
                    this.setTaskProgress( stage, progress );

                }
                experimentGroupIndex++;
            }

            watch.stop();
            if ( watch.getTotalTimeMillis() > 1000 ) {
                // This does not include getting the actual diff ex results.
                log.info( "Get information on conditions/analyses: " + watch.getTotalTimeMillis() );
            }
            return usedResultSets;
        }

        /**
         * @param searchResult
         */
        private void addGenesToSearchResultValueObject( DifferentialExpressionGenesConditionsValueObject searchResult ) {
            int geneGroupIndex = 0;
            for ( List<Gene> geneGroup : genes ) {
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

        private String constructConditionId( long resultSetId, long factorValueId ) {
            return "rs:" + resultSetId + "fv:" + factorValueId;
        }

        /**
         * @param resultSets
         * @param geneIds
         * @param arrayDesignsUsed
         * @param searchResult
         */
        private void fillBatchOfHeatmapCells( List<ExpressionAnalysisResultSet> resultSets, List<Long> geneIds,
                Collection<ArrayDesign> arrayDesignsUsed, DifferentialExpressionGenesConditionsValueObject searchResult ) {

            StopWatch watch = new StopWatch( "Fill diff ex heatmap cells" );
            watch.start( "DB query for hits" );

            // Main query for results.
            Map<Long, Map<Long, DiffExprGeneSearchResult>> resultSetToGeneResults = differentialExpressionResultService
                    .findDifferentialExpressionAnalysisResultIdsInResultSet( EntityUtils.getIds( resultSets ), geneIds,
                            getADIds( arrayDesignsUsed ) );
            watch.stop();

            Map<Long, ExpressionAnalysisResultSet> resultSetMap = EntityUtils.getIdMap( resultSets );

            watch.start( "Processing results from DB query" );
            for ( Entry<Long, Map<Long, DiffExprGeneSearchResult>> resultSetEntry : resultSetToGeneResults.entrySet() ) {

                Map<Long, DiffExprGeneSearchResult> geneToProbeResult = resultSetEntry.getValue();

                ExpressionAnalysisResultSet resultSet = resultSetMap.get( resultSetEntry.getKey() );
                assert resultSet != null;

                List<Long> probeAnalysisResultIds = new LinkedList<Long>();
                for ( DiffExprGeneSearchResult r : geneToProbeResult.values() ) {
                    probeAnalysisResultIds.add( r.getDifferentialExpressionAnalysisResultId() );
                }

                // FIXME don't fetch if not needed. But this is quite fast, per cycle
                // log.info( "Get the results we need from one result set: " + probeAnalysisResultIds.size() +
                // " to get." );
                Map<Long, DifferentialExpressionAnalysisResult> probeAnalysisResults = EntityUtils
                        .getIdMap( differentialExpressionResultService.load( probeAnalysisResultIds ) );
                // log.info( "done" );

                for ( Long geneId : geneIds ) {
                    Long probeResultId = null;

                    if ( geneToProbeResult.get( geneId ) != null ) {
                        probeResultId = geneToProbeResult.get( geneId ).getDifferentialExpressionAnalysisResultId();
                    }
                    DifferentialExpressionAnalysisResult deaResult = probeAnalysisResults.get( probeResultId );
                    if ( deaResult == null ) continue;

                    Double pValue = deaResult.getCorrectedPvalue();
                    if ( pValue == null ) {
                        // FIXME is this a magic number? Possibly we should treat such missing values as missing, not
                        // 1.0
                        pValue = 1.0;
                    }
                    if ( pValue.doubleValue() == 0.0 ) {
                        // FIXME is this a magic number?
                        pValue = 0.000000001; // 1e-9
                    }

                    int numberOfProbes = geneToProbeResult.get( geneId ).getNumberOfProbes();

                    int numberOfProbesDiffExpressed = geneToProbeResult.get( geneId ).getNumberOfProbesDiffExpressed();

                    markCellsBlack( resultSet, geneId, searchResult, pValue, numberOfProbes,
                            numberOfProbesDiffExpressed );

                    for ( ContrastResult cr : deaResult.getContrasts() ) {
                        FactorValue factorValue = cr.getFactorValue();
                        assert factorValue != null : "Null factor value for contrast with id" + cr.getId();
                        String conditionId = constructConditionId( resultSet.getId(), factorValue.getId() );
                        searchResult.addCell( geneId, conditionId, pValue, cr.getLogFoldChange(), numberOfProbes,
                                numberOfProbesDiffExpressed );
                    }

                }
            }
            watch.stop();
            log.info( watch.prettyPrint() );
        }

        /**
         * @param resultSets
         * @param geneIds
         * @param searchResult
         */
        private void fillHeatmapCells( List<ExpressionAnalysisResultSet> resultSets, List<Long> geneIds,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {
            this.setTaskProgress( "Processing request...", 10 );

            Collection<ArrayDesign> arrayDesignsUsed = new HashSet<ArrayDesign>();

            for ( ExpressionAnalysisResultSet rs : resultSets ) {
                arrayDesignsUsed.addAll( eeService.getArrayDesignsUsed( rs.getAnalysis().getExperimentAnalyzed() ) );
            }

            fillBatchOfHeatmapCells( resultSets, geneIds, arrayDesignsUsed, searchResult );

        }

        /**
         * @param analyses
         * @return
         */
        private List<DifferentialExpressionAnalysis> filterAnalyses( Collection<DifferentialExpressionAnalysis> analyses ) {
            List<DifferentialExpressionAnalysis> filteredAnalyses = new LinkedList<DifferentialExpressionAnalysis>();

            for ( DifferentialExpressionAnalysis analysis : analyses ) {
                if ( analysis == null ) continue; // skip if analysis was not run. FIXME is this possible?

                filteredAnalyses.add( analysis );
            }

            return filteredAnalyses;
        }

        /**
         * @param factors
         * @return
         */
        private ExperimentalFactor filterFactors( Collection<ExperimentalFactor> factors ) {
            // There should be only one factor.
            ExperimentalFactor factor = factors.iterator().next();
            return factor;
        }

        /**
         * @param factorValues
         * @param baselineFactorValueId
         * @return
         */
        private List<FactorValue> filterFactorValues( Collection<FactorValue> factorValues, long baselineFactorValueId ) {
            List<FactorValue> filteredFactorValues = new LinkedList<FactorValue>();

            for ( FactorValue factorValue : factorValues ) {
                if ( factorValue.getId() == baselineFactorValueId ) continue; // Skip baseline

                filteredFactorValues.add( factorValue );
            }

            return filteredFactorValues;
        }

        /**
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
                    log.error( "Possible Data Issue: resultSet.getBaselineGroup() returned null for result set wit ID="
                            + resultSet.getId() );
                    continue;
                }

                filteredResultSets.add( resultSet );
            }

            return filteredResultSets;
        }

        /**
         * @param resultSets
         * @return
         */
        private List<Long> getADIds( Collection<ArrayDesign> resultSets ) {
            List<Long> ids = new LinkedList<Long>();
            for ( ArrayDesign resultSet : resultSets ) {
                ids.add( resultSet.getId() );
            }
            return ids;
        }

        /*
         * Helper method to get factor values. TODO: Fix FactoValue class to return correct factor value in the first
         * place.
         */
        private String getFactorValueString( FactorValue fv ) {
            if ( fv == null ) return "null"; // FIXME is this a magic string?

            if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
                String fvString = "";
                for ( Characteristic c : fv.getCharacteristics() ) {
                    fvString += c.getValue() + " ";
                }
                return fvString;
            } else if ( fv.getMeasurement() != null ) {
                return fv.getMeasurement().getValue();
            } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
                return fv.getValue();
            } else
                return "absent "; // FIXME is this a magic string?
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
         * @param pValue
         * @param numProbes
         * @param numProbesDiffExpressed
         */
        private void markCellsBlack( ExpressionAnalysisResultSet resultSet, Long geneId,
                DifferentialExpressionGenesConditionsValueObject searchResult, double pValue, int numProbes,
                int numProbesDiffExpressed ) {

            /*
             * Note that if the resultSet has more than one experimental factor, it is an interaction term.
             */

            ExperimentalFactor experimentalFactor = resultSet.getExperimentalFactors().iterator().next();
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            for ( FactorValue factorValue : factorValues ) {
                String conditionId = constructConditionId( resultSet.getId(), factorValue.getId() );
                searchResult.addBlackCell( geneId, conditionId, pValue, numProbes, numProbesDiffExpressed );
            }
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
     * ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchService#getDiffExpSearchResult(java.lang
     * .String)
     */
    @Override
    public DifferentialExpressionGenesConditionsValueObject getDiffExpSearchResult( String taskId ) {
        DifferentialExpressionGenesConditionsValueObject result = null;

        try {
            DifferentialExpressionSearchTask diffExpSearchTask = ( DifferentialExpressionSearchTask ) this.diffExpSearchTasksCache
                    .get( taskId ).getObjectValue();

            ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
            Future<DifferentialExpressionGenesConditionsValueObject> backgroundTask = singleThreadExecutor
                    .submit( diffExpSearchTask );
            singleThreadExecutor.shutdown();

            result = backgroundTask.get(); // blocks
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        } catch ( ExecutionException e ) {
            throw new RuntimeException( e );
        } finally {
            this.diffExpSearchTasksCache.remove( taskId );
        }
        return result;
    }

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
            return diffExpSearchTask.getTaskProgress();
        }
        return new TaskProgress( "Completed", 100.0 );
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
        DifferentialExpressionSearchTask diffExpSearchTask = new DifferentialExpressionSearchTask( genes, experiments,
                geneGroupNames, experimentGroupNames );

        String taskId = UUID.randomUUID().toString();
        this.diffExpSearchTasksCache.put( new Element( taskId, diffExpSearchTask ) );

        return taskId;
    }

    @PostConstruct
    protected void init() {
        this.diffExpSearchTasksCache = new Cache( "DifferentialExpressionVisSearchTasks", 300, false, false, 3600, 3600 );
        this.cacheManager.addCache( this.diffExpSearchTasksCache );
    }

}