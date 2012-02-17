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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDaoImpl.DiffExprGeneSearchResult;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject.Condition;

/**
 * TODO Document Me
 * 
 * @author anton
 * @version $Id$
 */
@Component
public class DifferentialExpressionGeneConditionSearchService {
    protected static Log log = LogFactory.getLog( DifferentialExpressionGeneConditionSearchService.class.getName() );

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao differentialExpressionAnalysisResultDao;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private CacheManager cacheManager;

    private Cache diffExpSearchTasksCache;

    public static class TaskProgress {
        private double progressPercent;
        private String currentStage;

        public TaskProgress( String stage, double percent ) {
            this.currentStage = stage;
            this.progressPercent = percent;
        }

        public double getProgressPercent() {
            return this.progressPercent;
        }

        public String getCurrentStage() {
            return this.currentStage;
        }
    }

    @PostConstruct
    protected void init() {
        this.diffExpSearchTasksCache = new Cache( "DifferentialExpressionVisSearchTasks", 300, false, false, 3600, 3600 );
        this.cacheManager.addCache( this.diffExpSearchTasksCache );
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

        public synchronized TaskProgress getTaskProgress() {
            return new TaskProgress( this.taskProgressStage, this.taskProgressPercent ); // I think this is safe only
                                                                                         // because String is immutable
                                                                                         // and double is copied by
                                                                                         // value. FIXME
        }

        private synchronized void setTaskProgress( String stage, double percent ) {
            this.taskProgressStage = stage;
            this.taskProgressPercent = percent;
        }

        public DifferentialExpressionGenesConditionsValueObject call() {
            StopWatch watch = new StopWatch( "createGenesConditionsValueObject" );
            watch.start();

            // Order of calls matters (for now). Can be made more robust if needed.
            DifferentialExpressionGenesConditionsValueObject searchResult = new DifferentialExpressionGenesConditionsValueObject();

            addGenesToSearchResultValueObject( genes, geneGroupNames, searchResult );

            List<ExpressionAnalysisResultSet> resultSets = addConditionsToSearchResultValueObject( experiments,
                    experimentGroupNames, searchResult );

            fillHeatmapCells( resultSets, getGeneIds( searchResult.getGenes() ), searchResult );

            watch.stop();
            log.info( watch.prettyPrint() );
            return searchResult;
        }

        private void addGenesToSearchResultValueObject( List<List<Gene>> genes, List<String> geneGroupNames,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {
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

        private List<ExpressionAnalysisResultSet> addConditionsToSearchResultValueObject(
                List<Collection<ExpressionExperiment>> experimentGroups, List<String> experimentGroupNames,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {

            StopWatch watch = new StopWatch( "addConditionsToSearchResultValueObject" );

            List<ExpressionAnalysisResultSet> usedResultSets = new LinkedList<ExpressionAnalysisResultSet>();

            int experimentGroupIndex = 0;
            for ( Collection<ExpressionExperiment> experimentGroup : experimentGroups ) {

                String stage = "Loading " + experimentGroupNames.get( experimentGroupIndex ) + " experiments...";
                double progress = 0.0;
                double progressStep = 100.0 / experimentGroup.size();
                this.setTaskProgress( stage, progress );

                for ( ExpressionExperiment experiment : experimentGroup ) {

                    watch.start( "Load analyses for " + experiment.getShortName() );
                    List<DifferentialExpressionAnalysis> analyses = filterAnalyses( differentialExpressionAnalysisService
                            .getAnalyses( experiment ) );
                    watch.stop();
                    for ( DifferentialExpressionAnalysis analysis : analyses ) {
                        watch.start( "Thaw analysis " + analysis.getId() );
                        differentialExpressionAnalysisService.thaw( analysis );
                        watch.stop();

                        List<ExpressionAnalysisResultSet> resultSets = filterResultSets( analysis.getResultSets() );
                        usedResultSets.addAll( resultSets );

                        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
                            ExperimentalFactor factor = filterFactors( resultSet.getExperimentalFactors() );
                            Collection<FactorValue> factorValues = filterFactorValues( factor.getFactorValues(),
                                    resultSet.getBaselineGroup().getId() );

                            for ( FactorValue factorValue : factorValues ) {

                                Condition condition = searchResult.new Condition();

                                condition.id = "rs:" + resultSet.getId() + "fv:" + factorValue.getId();
                                condition.experimentGroupName = experimentGroupNames.get( experimentGroupIndex );
                                condition.experimentGroupIndex = experimentGroupIndex;
                                condition.datasetShortName = experiment.getShortName();
                                condition.datasetName = experiment.getName();
                                condition.datasetId = experiment.getId();
                                condition.analysisId = analysis.getId();
                                condition.resultSetId = resultSet.getId();

                                int up = -1, down = -1, total = -1;
                                log.info( "Got " + resultSet.getHitListSizes().size()
                                        + " hit list sizes for result set with id " + resultSet.getId() );
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

                                // if (up == -1) {
                                // up = differentialExpressionAnalysisService.countUpregulated( resultSet, 0.05 );
                                // }
                                // if (down == -1) {
                                // up = differentialExpressionAnalysisService.countDownregulated( resultSet, 0.05 );
                                // }
                                if ( total == -1 ) {
                                    total = differentialExpressionAnalysisService.countProbesMeetingThreshold(
                                            resultSet, 0.5 );
                                }

                                watch.start( "getProcessedExpressionVectorCount" );
                                condition.numberOfProbesOnArray = expressionExperimentDao
                                        .getProcessedExpressionVectorCount( experiment );
                                watch.stop();
                                condition.numberDiffExpressedProbes = total;
                                condition.numberDiffExpressedProbesUp = up;
                                condition.numberDiffExpressedProbesDown = down;
                                condition.baselineFactorValueId = resultSet.getBaselineGroup().getId();
                                condition.baselineFactorValue = getFactorValueString( resultSet.getBaselineGroup() );
                                condition.factorName = factor.getName();
                                condition.contrastFactorValue = getFactorValueString( factorValue );
                                condition.factorDescription = factor.getDescription();
                                condition.factorId = factor.getId();
                                condition.factorCategory = ( factor.getCategory() == null ) ? "null" : factor
                                        .getCategory().getCategory();

                                if ( condition.numberOfProbesOnArray < condition.numberDiffExpressedProbes ) {
                                    log.error( "The data is messed up. More diff expressed probes than probes on array" );
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

            log.info( watch.prettyPrint() );
            return usedResultSets;
        }

        private List<DifferentialExpressionAnalysis> filterAnalyses( Collection<DifferentialExpressionAnalysis> analyses ) {
            List<DifferentialExpressionAnalysis> filteredAnalyses = new LinkedList<DifferentialExpressionAnalysis>();

            for ( DifferentialExpressionAnalysis analysis : analyses ) {
                if ( analysis == null ) continue; // skip if analysis was not run

                filteredAnalyses.add( analysis );
            }

            return filteredAnalyses;
        }

        private List<ExpressionAnalysisResultSet> filterResultSets( Collection<ExpressionAnalysisResultSet> resultSets ) {
            List<ExpressionAnalysisResultSet> filteredResultSets = new LinkedList<ExpressionAnalysisResultSet>();

            for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
                // Skip interactions.
                if ( resultSet.getExperimentalFactors().size() != 1 ) continue;
                // Skip batch effect ones.
                if ( isBatch( resultSet ) ) continue;
                // Skip if baseline is not specified.
                if ( resultSet.getBaselineGroup() == null ) {
                    log.error( "Possible Data Issue: resultSet.getBaselineGroup() returned null." );
                    continue;
                }

                filteredResultSets.add( resultSet );
            }

            return filteredResultSets;
        }

        private ExperimentalFactor filterFactors( Collection<ExperimentalFactor> factors ) {
            // There should be only one factor.
            ExperimentalFactor factor = factors.iterator().next();
            return factor;
        }

        private List<FactorValue> filterFactorValues( Collection<FactorValue> factorValues, long baselineFactorValueId ) {
            List<FactorValue> filteredFactorValues = new LinkedList<FactorValue>();

            for ( FactorValue factorValue : factorValues ) {
                if ( factorValue.getId() == baselineFactorValueId ) continue; // Skip baseline

                filteredFactorValues.add( factorValue );
            }

            return filteredFactorValues;
        }

        private List<Long> getGeneBatch( List<Long> geneIds, int batchIndex ) {
            final int BATCH_SIZE = 20;
            int fromIndex = batchIndex * BATCH_SIZE;
            if ( fromIndex >= geneIds.size() ) return null;
            int toIndex = Math.min( fromIndex + BATCH_SIZE, geneIds.size() );
            return geneIds.subList( fromIndex, toIndex );
        }

        private List<ExpressionAnalysisResultSet> getResultSetBatch( List<ExpressionAnalysisResultSet> resultSets,
                int batchIndex ) {
            final int BATCH_SIZE = 1;
            int fromIndex = BATCH_SIZE * batchIndex;
            if ( fromIndex >= resultSets.size() ) return null;
            int toIndex = Math.min( fromIndex + BATCH_SIZE, resultSets.size() );
            return resultSets.subList( fromIndex, toIndex );
        }

        private void fillHeatmapCells( List<ExpressionAnalysisResultSet> resultSets, List<Long> geneIds,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {
            double progressStepSize = 100.0 / resultSets.size();
            double progress = 0.0;
            this.setTaskProgress( "Processing request...", progress );

            int resultSetBatchIndex = 0;
            List<ExpressionAnalysisResultSet> resultSetBatch = getResultSetBatch( resultSets, resultSetBatchIndex );
            while ( resultSetBatch != null ) {
                int geneBatchIndex = 0;
                List<Long> geneBatch = getGeneBatch( geneIds, geneBatchIndex );
                while ( geneBatch != null ) {

                    fillBatchOfHeatmapCells( resultSetBatch, geneBatch, searchResult );

                    geneBatch = getGeneBatch( geneIds, geneBatchIndex );
                    geneBatchIndex++;
                }
                resultSetBatchIndex++;

                progress += progressStepSize * resultSetBatch.size();
                this.setTaskProgress( "Processing request...", progress );

                resultSetBatch = getResultSetBatch( resultSets, resultSetBatchIndex );
            }
        }

        private void fillBatchOfHeatmapCells( List<ExpressionAnalysisResultSet> resultSetBatch, List<Long> geneIdBatch,
                DifferentialExpressionGenesConditionsValueObject searchResult ) {
            ExpressionAnalysisResultSet resultSet = resultSetBatch.iterator().next();

            StopWatch watch = new StopWatch( "" );
            Collection<ArrayDesign> arrayDesignsUsed = new ArrayList<ArrayDesign>();
            watch.start( "Get array designs used." );

            for ( ExpressionAnalysisResultSet rs : resultSetBatch ) {
                arrayDesignsUsed.addAll( expressionExperimentDao.getArrayDesignsUsed( ( ExpressionExperiment ) rs
                        .getAnalysis().getExperimentAnalyzed() ) );
            }
            watch.stop();

            watch.start( "Batched call" );
            Map<Long, DiffExprGeneSearchResult> geneToProbeResult = differentialExpressionResultService
                    .findProbeAnalysisResultIdsInResultSet( resultSet.getId(), geneIdBatch, getADIds( arrayDesignsUsed ) );
            watch.stop();

            List<Long> probeAnalysisResultIds = new LinkedList<Long>();
            for ( DiffExprGeneSearchResult r : geneToProbeResult.values() ) {
                probeAnalysisResultIds.add( r.getProbeAnalysisResultId() );
            }

            watch.start( "Loading contrasts" );
            Map<Long, DifferentialExpressionAnalysisResult> probeAnalysisResults = differentialExpressionAnalysisResultDao
                    .loadMultiple( probeAnalysisResultIds );
            watch.stop();

            for ( Long geneId : geneIdBatch ) {
                Long probeResultId = null;
                if ( geneToProbeResult.get( geneId ) != null ) {
                    probeResultId = geneToProbeResult.get( geneId ).getProbeAnalysisResultId();
                }
                DifferentialExpressionAnalysisResult deaResult = probeAnalysisResults.get( probeResultId );
                if ( deaResult == null ) continue;

                Double pValue = deaResult.getCorrectedPvalue();
                if ( pValue == null ) {
                    pValue = 1.0;
                }
                if ( pValue.doubleValue() == 0.0 ) {
                    pValue = 0.000000001;
                }
                markCellsBlack( resultSet, geneId, searchResult, pValue, geneToProbeResult.get( geneId )
                        .getNumberOfProbes(), geneToProbeResult.get( geneId ).getNumberOfProbesDiffExpressed() );

                for ( ContrastResult cr : deaResult.getContrasts() ) {
                    // double visualizationValue = calculateVisualizationValueBasedOnPvalue ( cr.getPvalue() );

                    String conditionId = constructConditionId( resultSet.getId(), cr.getFactorValue().getId() );
                    searchResult.addCell( geneId, conditionId, pValue, cr.getLogFoldChange(),
                            geneToProbeResult.get( geneId ).getNumberOfProbes(), geneToProbeResult.get( geneId )
                                    .getNumberOfProbesDiffExpressed() );
                }
            }
            log.info( watch.prettyPrint() );
        }

        private void markCellsBlack( ExpressionAnalysisResultSet resultSet, Long geneId,
                DifferentialExpressionGenesConditionsValueObject searchResult, double pValue, int numProbes,
                int numProbesDiffExpressed ) {
            for ( FactorValue factorValue : resultSet.getExperimentalFactors().iterator().next().getFactorValues() ) {
                String conditionId = constructConditionId( resultSet.getId(), factorValue.getId() );
                searchResult.addBlackCell( geneId, conditionId, pValue, numProbes, numProbesDiffExpressed );
            }
        }

        private String constructConditionId( long resultSetId, long factorValueId ) {
            return "rs:" + resultSetId + "fv:" + factorValueId;
        }

        private List<Long> getIds( Collection<ExpressionAnalysisResultSet> resultSets ) {
            List<Long> ids = new LinkedList<Long>();
            for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
                ids.add( resultSet.getId() );
            }
            return ids;
        }

        private List<Long> getGeneIds(
                Collection<ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject.Gene> genes ) {
            List<Long> ids = new LinkedList<Long>();
            for ( ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject.Gene gene : genes ) {
                ids.add( gene.getId() );
            }
            return ids;
        }

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
            if ( fv == null ) return "null";

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
                return "absent ";
        }

        private boolean isBatch( ExpressionAnalysisResultSet resultSet ) {
            for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
                if ( ExperimentalDesignUtils.isBatch( factor ) ) {
                    return true;
                }
            }
            return false;
        }
    }

    public String scheduleDiffExpSearchTask( List<List<Gene>> genes,
            List<Collection<ExpressionExperiment>> experiments, List<String> geneGroupNames,
            List<String> experimentGroupNames ) {
        DifferentialExpressionSearchTask diffExpSearchTask = new DifferentialExpressionSearchTask( genes, experiments,
                geneGroupNames, experimentGroupNames );

        String taskId = UUID.randomUUID().toString();
        this.diffExpSearchTasksCache.put( new Element( taskId, diffExpSearchTask ) );

        return taskId;
    }

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( ExecutionException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            this.diffExpSearchTasksCache.remove( taskId );
        }
        return result;
    }

    public TaskProgress getDiffExpSearchTaskProgress( String taskId ) {
        DifferentialExpressionSearchTask diffExpSearchTask = ( DifferentialExpressionSearchTask ) this.diffExpSearchTasksCache
                .get( taskId ).getObjectValue();
        return diffExpSearchTask.getTaskProgress();
    }

}