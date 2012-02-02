/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.analysis.preprocess.batcheffects.ExpressionExperimentBatchCorrectionService;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Encapsulates steps that are done to expression data sets after they are loaded, or which can be triggered by certain
 * later events in the lifecycle of a data set. These include:
 * <ol>
 * <li>Reprocess from raw data?
 * <li>Computing missing values
 * <li>Switching to a merged array design and merging vectors
 * <li>Creation of "processed" vectors
 * <li>(Re)normalization
 * <li>Getting information on batches
 * <li>PCA
 * <li>Computing sample-wise correlation matrices
 * <li>Outlier detection
 * <li>Batch correction
 * <li>Populating the experimental design (guesses)?
 * <li>Ordering of vectors with respect to the experimental design? [probably not, this isn't a problem]
 * <li>Autotagger
 * </ol>
 * <p>
 * WORK IN PROGRESS
 * </p
 * .
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class PreprocessorService {

    private static Log log = LogFactory.getLog( PreprocessorService.class );

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService = null;

    @Autowired
    private ProcessedExpressionDataVectorService processedDataService = null;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValueService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private BatchInfoPopulationService batchInfoPopulationService;

    @Autowired
    ExpressionExperimentBatchCorrectionService batchCorrectionService;

    /**
     * @param ee
     */
    // private void process( ExpressionExperiment ee ) {
    //
    // processForMissingValues( ee );
    //
    // /*
    // * Normalize here?
    // */
    //

    //
    // batchInfoPopulationService.fillBatchInformation( ee );
    // svdService.svd( ee );
    //
    // /*
    // * Batch correct here.
    // */
    //
    // sampleCoexpressionMatrixService.getSampleCorrelationMatrix( ee );
    // }

    public void createProcessedVectors( ExpressionExperiment ee ) {
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
    }

    /**
     * @param ee
     * @return
     */
    private boolean processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            log.warn( "Skipping postprocessing because experiment uses "
                    + "multiple array types. Please check valid entry and run postprocessing separately." );
        }

        ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
        boolean wasProcessed = false;

        TechnologyType tt = arrayDesignUsed.getTechnologyType();
        if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
            log.info( ee + " uses a two-color array design, processing for missing values ..." );
            ee = expressionExperimentService.thawLite( ee );
            twoChannelMissingValueService.computeMissingValues( ee );
            wasProcessed = true;
        }

        return wasProcessed;
    }

    /**
     * @param ee
     */
    public void process( ExpressionExperiment ee ) {

        ee = expressionExperimentService.thaw( ee );

        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            log.warn( "Skipping postprocessing because experiment uses "
                    + "multiple array types. Please check valid entry and run postprocessing separately." );
        }

        ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
        processForMissingValues( ee, arrayDesignUsed );
        processForSampleCorrelation( ee );
        processForPca( ee );
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
            ee = expressionExperimentService.thawLite( ee );
            twoChannelMissingValueService.computeMissingValues( ee );
            wasProcessed = true;
        }

        return wasProcessed;
    }

    /**
     * @param ee
     */
    private void processForPca( ExpressionExperiment ee ) {
        svdService.svd( ee );
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     * 
     * @param ee
     */
    private void processForSampleCorrelation( ExpressionExperiment ee ) {
        sampleCoexpressionMatrixService.findOrCreate( ee );
    }

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;
}
