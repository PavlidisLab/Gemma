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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.preprocess.batcheffects.ExpressionExperimentBatchCorrectionService;
import ubic.gemma.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchCorrectionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Encapsulates steps that are done to expression data sets after they are loaded, or which can be triggered by certain
 * later events in the lifecycle of a data set. These include:
 * <ol>
 * <li>Deleting old analysis files and results, as these are invalidated by the subsequent steps.S
 * <li>Computing missing values - Basic processing
 * <li>Creation of "processed" vectors
 * <li>PCA
 * <li>Computing sample-wise correlation matrices for diagnostic plot
 * <li>Computing mean-variance data for diagnostic plots
 * </ol>
 * <p>
 * WORK IN PROGRESS
 * </p>
 * <p>
 * Other elements that can be considered
 * <ol>
 * <li>Reprocess from raw data? - not yet
 * <li>Switching to a merged array design and merging vectors
 * <li>(Re)normalization
 * <li>Getting information on batches
 * <li>Outlier detection
 * <li>Batch correction -- shoud be done after the experimental design is done, and after batch info has been obtained,
 * and after outliers have been removed.
 * <li>Ordering of vectors with respect to the experimental design? [probably not, this isn't a big problem]
 * <li>Populating the experimental design (guesses)?
 * <li>Autotagger .
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class PreprocessorServiceImpl implements PreprocessorService {

    private static Log log = LogFactory.getLog( PreprocessorServiceImpl.class );

    @Autowired
    private DifferentialExpressionAnalyzerService analyzerService;

    @Autowired
    private ExpressionDataFileService dataFileService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ProcessedExpressionDataVectorService dataVectorService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionExperimentBatchCorrectionService expressionExperimentBatchCorrectionService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private MeanVarianceService meanVarianceService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Autowired
    private SVDServiceHelper svdService;

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValueService;

    @Autowired
    private AuditTrailService auditTrailService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.PreprocessorService#batchCorrect(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment batchCorrect( ExpressionExperiment ee ) throws PreprocessingException {

        /*
         * This leaves the raw data alone; it updates the processed data.
         */

        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new PreprocessingException( "Can only batch-correct data for an experiment that uses one platform; "
                    + "you must switch/merge first." );
        }

        ee = expressionExperimentService.thawLite( ee );

        boolean correctable = expressionExperimentBatchCorrectionService.checkCorrectability( ee );

        if ( !correctable ) {
            throw new PreprocessingException( ee.getShortName()
                    + " could not be batch-corrected (either no batch information, or invalid design)" );
        }

        try {

            Collection<ProcessedExpressionDataVector> vecs = dataVectorService.getProcessedDataVectors( ee );

            if ( vecs == null || vecs.isEmpty() ) {
                this.processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
                vecs = dataVectorService.getProcessedDataVectors( ee );
            }

            assert vecs != null;

            dataVectorService.thaw( vecs );

            ExpressionDataDoubleMatrix correctedData = expressionExperimentBatchCorrectionService
                    .comBat( new ExpressionDataDoubleMatrix( vecs ) );

            /*
             * FIXME: this produces two plots that can be used as diagnostics, we could link them into this.
             */

            if ( correctedData == null || correctedData.rows() != vecs.size() ) {
                throw new PreprocessingException( ee.getShortName()
                        + " could not be batch-corrected (matrix returned by ComBat had wrong number of rows)" );
            }

            Collection<QuantitationType> qts = correctedData.getQuantitationTypes();
            assert !qts.isEmpty();

            if ( qts.size() > 1 ) {
                throw new IllegalArgumentException( "Only support a single quantitation type" );
            }

            QuantitationType batchCorrectedQt = qts.iterator().next();
            assert batchCorrectedQt.getIsBatchCorrected();

            // Convert to vectors
            Collection<ProcessedExpressionDataVector> newVecs = new HashSet<>( correctedData.toProcessedDataVectors() );
            processedExpressionDataVectorCreateService.createProcessedDataVectors( ee, newVecs );

            /*
             * FIXME this is temporarily a generic event switch it once code is synced with production.
             */
            // AuditEventType eventType = BatchCorrectionEvent.Factory.newInstance();
            AuditEventType eventType = DataReplacedEvent.Factory.newInstance();
            auditTrailService.addUpdateEvent( ee, eventType, "ComBat batch correction" );

            removeInvalidatedData( ee );
            return processExceptForVectorCreate( ee );

        } catch ( Exception e ) {
            throw new PreprocessingException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.PreprocessorService#process(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment process( ExpressionExperiment ee ) throws PreprocessingException {

        ee = expressionExperimentService.thawLite( ee );

        try {
            removeInvalidatedData( ee );
            processForMissingValues( ee );
            processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

            return processExceptForVectorCreate( ee );
        } catch ( Exception e ) {
            throw new PreprocessingException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.PreprocessorService#process(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, boolean)
     */
    @Override
    public ExpressionExperiment process( ExpressionExperiment ee, boolean light ) throws PreprocessingException {
        if ( light ) {
            try {
                removeInvalidatedData( ee );
                processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
                processForSampleCorrelation( ee );
                processForMeanVarianceRelation( ee );
                processForPca( ee );
                // analyzerService.deleteAnalyses( ee ); ??
            } catch ( Exception e ) {
                throw new PreprocessingException( e );
            }
        } else {
            process( ee );
        }

        return ee;

    }

    /**
     * @param ee
     * @return
     */
    private ExpressionExperiment processExceptForVectorCreate( ExpressionExperiment ee ) {
        // // refresh into context.
        ee = expressionExperimentService.thawLite( ee );

        assert ee.getNumberOfDataVectors() != null;

        /*
         * Redo any old diff ex analyses
         */
        Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        if ( !oldAnalyses.isEmpty() ) {

            log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
            Collection<DifferentialExpressionAnalysis> results = new HashSet<>();
            for ( DifferentialExpressionAnalysis copyMe : oldAnalyses ) {
                try {
                    results.addAll( this.analyzerService.redoAnalysis( ee, copyMe ) );
                } catch ( Exception e ) {
                    log.error( "Could not redo analysis: " + " " + copyMe + ": " + e.getMessage() );
                }
            }
        }

        processForSampleCorrelation( ee );
        processForMeanVarianceRelation( ee );
        processForPca( ee );

        expressionExperimentService.update( ee );
        assert ee.getNumberOfDataVectors() != null;
        return ee;
    }

    /**
     * Create the scatter plot to evaluate heteroscedasticity.
     * 
     * @param eeId
     */
    private void processForMeanVarianceRelation( ExpressionExperiment ee ) {
        try {
            meanVarianceService.findOrCreate( ee );
        } catch ( Exception e ) {
            log.error( "Could not compute mean-variance relation: " + e.getMessage() );
        }
    }

    /**
     * @param ee
     * @return
     */
    private boolean processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            throw new UnsupportedOperationException( "Skipping postprocessing because experiment uses "
                    + "multiple platform types. Please check valid entry and run postprocessing separately." );
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
    private void processForPca( ExpressionExperiment ee ) {
        try {
            svdService.svd( ee );
        } catch ( Exception e ) {
            log.error( "SVD could not be performed: " + e.getMessage() );
        }
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     * 
     * @param ee
     */
    private void processForSampleCorrelation( ExpressionExperiment ee ) {
        try {
            sampleCoexpressionMatrixService.findOrCreate( ee );
        } catch ( Exception e ) {
            log.error( "SampleCorrelation could not be computed: " + e.getMessage() );
        }
    }

    /**
     * @param expExp
     */
    private void removeInvalidatedData( ExpressionExperiment expExp ) {

        try {
            dataFileService.deleteAllFiles( expExp );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
