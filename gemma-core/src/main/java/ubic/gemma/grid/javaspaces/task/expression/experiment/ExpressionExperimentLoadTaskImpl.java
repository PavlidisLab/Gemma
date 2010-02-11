/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.grid.javaspaces.task.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentLoadTaskImpl extends BaseSpacesTask implements ExpressionExperimentLoadTask {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    private GeoDatasetService geoDatasetService = null;
    ArrayExpressLoadService arrayExpressLoadService;
    ExpressionExperimentService eeService;
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;
    TwoChannelMissingValues twoChannelMissingValueService;
    ExpressionDataMatrixService expressionDataMatrixService;

 

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.SpacesTask#execute(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public TaskResult execute( ExpressionExperimentLoadTaskCommand command ) {
        ExpressionExperimentLoadTaskCommand jsEeLoadCommand = command;

        SpacesProgressAppender spacesProgressAppender = super.initProgressAppender( this.getClass() );

        String accession = jsEeLoadCommand.getAccession();
        boolean loadPlatformOnly = jsEeLoadCommand.isLoadPlatformOnly();
        boolean doSampleMatching = !jsEeLoadCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = jsEeLoadCommand.isAggressiveQtRemoval();
        boolean splitIncompatiblePlatforms = jsEeLoadCommand.isSplitIncompatiblePlatforms();
        boolean allowSuperSeriesLoad = jsEeLoadCommand.isAllowSuperSeriesLoad();

        TaskResult result = new TaskResult();
        if ( jsEeLoadCommand.isArrayExpress() ) {
            ExpressionExperiment dataset = arrayExpressLoadService.load( accession, jsEeLoadCommand
                    .getArrayDesignName(), jsEeLoadCommand.isAllowArrayExpressDesign() );
            ExpressionExperiment minimalDataset = null;
            if ( dataset != null ) {
                /* Don't send the full experiment to the space. Instead, create a minimal result. */
                minimalDataset = ExpressionExperiment.Factory.newInstance();
                minimalDataset.setId( dataset.getId() );
                minimalDataset.setName( dataset.getName() );
                minimalDataset.setDescription( dataset.getDescription() );
            }
            result.setAnswer( minimalDataset );
        } else if ( loadPlatformOnly ) {
            Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accession, true, doSampleMatching,
                    aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad );
            ArrayList<ArrayDesign> minimalDesigns = null;
            if ( arrayDesigns != null ) {
                /* Don't send the full array designs to space. Instead, create a minimal result. */
                minimalDesigns = new ArrayList<ArrayDesign>();
                for ( ArrayDesign ad : arrayDesigns ) {
                    ArrayDesign minimalDesign = ArrayDesign.Factory.newInstance();
                    minimalDesign.setId( ad.getId() );
                    minimalDesign.setName( ad.getName() );
                    minimalDesign.setDescription( ad.getDescription() );

                    minimalDesigns.add( minimalDesign );
                }
            }
            result.setAnswer( minimalDesigns );
        } else {
            Collection<ExpressionExperiment> datasets = geoDatasetService.fetchAndLoad( accession, loadPlatformOnly,
                    doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad );

            log.info( "Loading done, starting postprocessing" );
            postProcess( datasets );

            ArrayList<ExpressionExperiment> minimalDatasets = null;
            if ( datasets != null ) {
                /* Don't send the full experiments to space. Instead, create a minimal result. */
                minimalDatasets = new ArrayList<ExpressionExperiment>();
                for ( ExpressionExperiment ee : datasets ) {
                    ExpressionExperiment minimalDataset = ExpressionExperiment.Factory.newInstance();
                    minimalDataset.setId( ee.getId() );
                    minimalDataset.setName( ee.getName() );
                    minimalDataset.setDescription( ee.getDescription() );

                    minimalDatasets.add( minimalDataset );
                }
            }
            result.setAnswer( minimalDatasets );
        }
        result.setTaskID( super.taskId );
        log.info( "Task execution complete ... returning result for task with id " + result.getTaskID() );

        super.tidyProgress( spacesProgressAppender );

        return result;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    /**
     * Do missing value and processed vector creation steps.
     * 
     * @param ees
     */
    private void postProcess( Collection<ExpressionExperiment> ees ) {
        log.info( "Postprocessing ..." );
        for ( ExpressionExperiment ee : ees ) {

            Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( ee );
            if ( arrayDesignsUsed.size() > 1 ) {
                log.warn( "Skipping postprocessing because experiment uses "
                        + "multiple array types. Please check valid entry and run postprocessing separately." );
            }

            ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
            processForMissingValues( ee, arrayDesignUsed );
            Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorCreateService
                    .computeProcessedExpressionData( ee );

            ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( ee,
                    new FilterConfig(), dataVectors );
            ExpressionDataSampleCorrelation.process( datamatrix, ee );
        }
    }

    /**
     * @param ee
     * @return
     */
    private boolean processForMissingValues( ExpressionExperiment ee, ArrayDesign design ) {

        boolean wasProcessed = false;

        TechnologyType tt = design.getTechnologyType();
        if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
            log.info( ee + " uses a two-color array design, processing for missing values ..." );
            eeService.thawLite( ee );
            twoChannelMissingValueService.computeMissingValues( ee );
            wasProcessed = true;
        }

        return wasProcessed;
    }

    /**
     * @param arrayExpressLoadService the arrayExpressLoadService to set
     */
    public void setArrayExpressLoadService( ArrayExpressLoadService arrayExpressLoadService ) {
        this.arrayExpressLoadService = arrayExpressLoadService;
    }

    /**
     * @param geoDatasetService
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
        this.geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
    }

    public void setProcessedExpressionDataVectorCreateService(
            ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService ) {
        this.processedExpressionDataVectorCreateService = processedExpressionDataVectorCreateService;
    }

    public void setTwoChannelMissingValueService( TwoChannelMissingValues twoChannelMissingValueService ) {
        this.twoChannelMissingValueService = twoChannelMissingValueService;
    }
    
    public void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService ) {
        this.expressionDataMatrixService = expressionDataMatrixService;
    }

}