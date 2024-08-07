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
package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchCorrectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchCorrectionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMeanVarianceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedSampleCorrelationAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

import java.util.Collection;
import java.util.LinkedList;

@Service
public class PreprocessorServiceImpl implements PreprocessorService {

    private static final Log log = LogFactory.getLog( PreprocessorServiceImpl.class );

    @Autowired
    private DifferentialExpressionAnalyzerService analyzerService;
    @Autowired
    private ExpressionDataFileService dataFileService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentBatchCorrectionService expressionExperimentBatchCorrectionService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private MeanVarianceService meanVarianceService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private SVDServiceHelper svdService;
    @Autowired
    private TwoChannelMissingValues twoChannelMissingValueService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;
    @Autowired
    private GeeqService geeqService;

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void process( ExpressionExperiment ee, boolean ignoreQuantitationMismatch, boolean ignoreDiagnosticsFailure ) throws PreprocessingException {
        StopWatch timer = new StopWatch();
        timer.start();
        removeInvalidatedData( ee ); // clear out old files
        processForMissingValues( ee ); // only relevant for two-channel arrays
        processVectorCreate( ee, ignoreQuantitationMismatch ); // key step
        batchCorrect( ee ); // will be a no-op in many cases
        processBatchInfo( ee ); // update status
        try {
            processDiagnostics( ee ); // PCA, GEEQ, MV etc.
        } catch ( PreprocessingException e ) {
            if ( ignoreDiagnosticsFailure ) {
                log.warn( String.format( "Processing diagnostics failed for %s, will attempt to update DEAs anyway, but the plots might be incorrect.", ee.getShortName() ), e );
            } else {
                throw e;
            }

        }
        updateDEAs( ee ); // if existing, redo it
        log.info( "Processing complete for " + ee.getShortName() + " in " + timer.getTime() / 1000 + " seconds" );
    }

    /**
     * If possible, batch correct the processed data vectors. This entails repeating the other preprocessing steps. But
     * it should only be run after the experimental design is set up, the batch information has been fetched, and (of
     * course) the data needed are already available.
     */
    private void batchCorrect( ExpressionExperiment ee ) throws PreprocessingException {
        if ( !expressionExperimentBatchCorrectionService.checkCorrectability( ee ) ) {
            return;
        }

        Collection<ProcessedExpressionDataVector> vecs = this.getProcessedExpressionDataVectors( ee );

        ExpressionDataDoubleMatrix correctedData = this.getCorrectedData( ee, vecs );

        // Convert to vectors (persist QT)
        processedExpressionDataVectorService.replaceProcessedDataVectors( ee, correctedData.toProcessedDataVectors() );

        auditTrailService.addUpdateEvent( ee, BatchCorrectionEvent.class, "ComBat batch correction", "" );
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void processDiagnostics( ExpressionExperiment ee ) throws PreprocessingException {
        this.processForSampleCorrelation( ee );
        this.processForMeanVarianceRelation( ee );
        this.processForPca( ee );
        // FIXME: OPT_MODE_ALL is overkill, but none of the options currently address the exact need. No big deal.
        geeqService.calculateScore( ee, GeeqService.ScoreMode.all );
    }

    private void processVectorCreate( ExpressionExperiment ee, boolean ignoreQuantitationMismatch ) throws PreprocessingException {
        try {
            processedExpressionDataVectorService.computeProcessedExpressionData( ee, ignoreQuantitationMismatch );
        } catch ( QuantitationMismatchException e ) {
            // wrap it in a runtime exception, which will result in a rollback of the current transaction
            throw new QuantitationMismatchPreprocessingException( ee, e );
        }
    }

    /**
     * Refresh the batch status of the data set.
     * @param ee
     */
    private void processBatchInfo( ExpressionExperiment ee ) {
        expressionExperimentReportService.recalculateExperimentBatchInfo( ee );
    }

    /**
     * Checks all the given expression experiments bio assays for outlier flags and returns them in a collection
     *
     * @param  ee the expression experiment to be checked
     * @return a collection of outlier details that contains all the outliers that the expression experiment is aware
     *            of.
     */
    private Collection<OutlierDetails> getAlreadyKnownOutliers( ExpressionExperiment ee ) {
        Collection<OutlierDetails> outliers = new LinkedList<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( ba.getIsOutlier() ) {
                OutlierDetails od = new OutlierDetails( ba );
                if ( !outliers.contains( od ) ) {
                    outliers.add( od );
                }
            }
        }
        return outliers;
    }

    /**
     * Update DEA if needed.
     */
    private void updateDEAs( ExpressionExperiment ee ) {
        assert ee.getNumberOfDataVectors() != null;

        /*
         * Redo any old diff ex analyses
         */
        Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                .findByExperiment( ee );

        if ( !oldAnalyses.isEmpty() ) {

            PreprocessorServiceImpl.log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
            for ( DifferentialExpressionAnalysis copyMe : oldAnalyses ) {
                this.analyzerService.redoAnalysis( ee, copyMe, true );
            }
        }
    }

    /**
     * Create the scatter plot to evaluate heteroscedasticity.
     */
    private void processForMeanVarianceRelation( ExpressionExperiment ee ) throws PreprocessingException {
        try {
            meanVarianceService.create( ee, true );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedMeanVarianceUpdateEvent.class, null, e );
            throw new PreprocessingException( ee, e );
        }
    }

    private void processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );

        // this is in a loop so we only issue a multiplatform warning if this is even relevant.
        for ( ArrayDesign arrayDesignUsed : arrayDesignsUsed ) {
            TechnologyType tt = arrayDesignUsed.getTechnologyType();
            if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {

                if ( arrayDesignsUsed.size() > 1 ) {
                    log.warn( "Skipping two-channel missing value computation: experiment uses multiple platform types. Please check valid entry and run postprocessing separately." );
                    return;
                }

                PreprocessorServiceImpl.log
                        .info( ee + " uses a two-color array design, processing for missing values ..." );
                twoChannelMissingValueService.computeMissingValues( ee );
            }
        }
    }

    private void processForPca( ExpressionExperiment ee ) throws SVDRelatedPreprocessingException {
        try {
            svdService.svd( ee );
        } catch ( SVDException e ) {
            auditTrailService.addUpdateEvent( ee, FailedPCAAnalysisEvent.class, null, e );
            throw new SVDRelatedPreprocessingException( ee, e );
        }
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     */
    private void processForSampleCorrelation( ExpressionExperiment ee ) throws SampleCoexpressionRelatedPreprocessingException {
        try {
            sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
        } catch ( RuntimeException e ) {
            auditTrailService.addUpdateEvent( ee, FailedSampleCorrelationAnalysisEvent.class, null, e );
            throw new SampleCoexpressionRelatedPreprocessingException( ee, e );
        }
    }

    private void removeInvalidatedData( ExpressionExperiment expExp ) {
        dataFileService.deleteAllFiles( expExp );
    }

    private void checkQuantitationType( ExpressionDataDoubleMatrix correctedData ) {
        Collection<QuantitationType> qts = correctedData.getQuantitationTypes();
        assert !qts.isEmpty();

        if ( qts.size() > 1 ) {
            throw new IllegalArgumentException( "Only support a single quantitation type" );
        }

        QuantitationType batchCorrectedQt = qts.iterator().next();
        assert batchCorrectedQt.getIsBatchCorrected();
    }

    private ExpressionDataDoubleMatrix getCorrectedData( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vecs ) throws PreprocessingException {

        /*
         * FIXME perhaps here we should remove rows that are going to be problematic?
         */

        ExpressionDataDoubleMatrix correctedData = expressionExperimentBatchCorrectionService
                .comBat( ee, new ExpressionDataDoubleMatrix( ee, vecs ) );

        /*
         * FIXME: this produces two plots that can be used as diagnostics, we could link them into this.
         */

        if ( correctedData == null ) {
            throw new PreprocessingException( ee, "could not be batch-corrected: ComBat did not found a suitable batch factor" );
        }

        if ( correctedData.rows() != vecs.size() ) {
            throw new PreprocessingException( ee, "could not be batch-corrected: matrix returned by ComBat had wrong number of rows" );
        }

        this.checkQuantitationType( correctedData );

        return correctedData;
    }

    /**
     *
     * @return processed data vectors; if they don't exist, create them. They will be thawed in either case.
     */
    private Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors( ExpressionExperiment ee ) {
        Collection<ProcessedExpressionDataVector> vecs = processedExpressionDataVectorService
                .getProcessedDataVectorsAndThaw( ee );
        if ( vecs.isEmpty() ) {
            log.info( String.format( "No processed vectors for %s, they will be computed from raw data...", ee ) );
            this.processedExpressionDataVectorService.computeProcessedExpressionData( ee );
            vecs = this.processedExpressionDataVectorService.getProcessedDataVectorsAndThaw( ee );
        }
        return vecs;
    }
}
