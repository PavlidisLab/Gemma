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
package ubic.gemma.tasks.analysis.expression;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * @author keshav
 * @version $Id$
 */
@Service
public class ExpressionExperimentLoadTaskImpl implements ExpressionExperimentLoadTask {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private GeoDatasetService geoDatasetService = null;

    @Autowired
    private ArrayExpressLoadService arrayExpressLoadService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValueService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#execute(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @TaskMethod
    public TaskResult execute( ExpressionExperimentLoadTaskCommand command ) {
        ExpressionExperimentLoadTaskCommand jsEeLoadCommand = command;

        String accession = jsEeLoadCommand.getAccession();
        boolean loadPlatformOnly = jsEeLoadCommand.isLoadPlatformOnly();
        boolean doSampleMatching = !jsEeLoadCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = jsEeLoadCommand.isAggressiveQtRemoval();
        boolean splitIncompatiblePlatforms = jsEeLoadCommand.isSplitIncompatiblePlatforms();
        boolean allowSuperSeriesLoad = jsEeLoadCommand.isAllowSuperSeriesLoad();
        boolean allowSubSeriesLoad = true; // FIXME

        TaskResult result;
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
            result = new TaskResult( command, minimalDataset );
        } else if ( loadPlatformOnly ) {
            Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService.fetchAndLoad(
                    accession, true, doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms );
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
            result = new TaskResult( command, minimalDesigns );
        } else {
            Collection<ExpressionExperiment> datasets = geoDatasetService.fetchAndLoad( accession, loadPlatformOnly,
                    doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad,
                    allowSubSeriesLoad );

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
            result = new TaskResult( command, minimalDatasets );
        }

        return result;
    }

    /**
     * Do missing value and processed vector creation steps.
     * 
     * @param ees
     */
    private void postProcess( Collection<ExpressionExperiment> ees ) {
        log.info( "Postprocessing ..." );
        for ( ExpressionExperiment ee : ees ) {

            postProcess( ee );
        }
    }

    /**
     * @param ee
     */
    private void postProcess( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            log.warn( "Skipping postprocessing because experiment uses "
                    + "multiple array types. Please check valid entry and run postprocessing separately." );
        }

        ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
        processForMissingValues( ee, arrayDesignUsed );
        processForSampleCorrelation( ee );
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     * 
     * @param ee
     */
    private void processForSampleCorrelation( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( ee );

        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( ee, new FilterConfig(),
                dataVectors );
        ExpressionDataSampleCorrelation.process( datamatrix, ee );
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

}