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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchCorrectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchCorrectionEvent;
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
 * Other elements that can be considered
 * <ol>
 * <li>Reprocess from raw data? - not yet
 * <li>Switching to a merged array design and merging vectors
 * <li>(Re)normalization
 * <li>Getting information on batches
 * <li>Outlier detection
 * <li>Batch correction -- should be done after the experimental design is done, and after batch info has been obtained,
 * and after outliers have been removed.
 * <li>Ordering of vectors with respect to the experimental design? [probably not, this isn't a big problem]
 * <li>Populating the experimental design (guesses)?
 * <li>Autotagger
 * </ol>
 *
 * @author paul
 */
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
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;
    @Autowired
    private GeeqService geeqService;

    @Override
    @Transactional
    public void process( ExpressionExperiment ee ) throws PreprocessingException {
        process( ee, false );
    }

    @Override
    @Transactional
    public void process( ExpressionExperiment ee, boolean detectScaleFromData ) throws PreprocessingException {
        ee = expressionExperimentService.thaw( ee );
        removeInvalidatedData( ee );
        processForMissingValues( ee );
        processVectorCreate( ee, detectScaleFromData );
        batchCorrect( ee );
    }

    @Override
    @Transactional
    public void processLight( ExpressionExperiment ee ) throws PreprocessingException {
        // we dont do processForMissingValues missing values
        ee = expressionExperimentService.thaw( ee );
        removeInvalidatedData( ee );
        processVectorCreate( ee, false );
        processDiagnostics( ee );
        processBatchInfo( ee );
    }

    /**
     * If possible, batch correct the processed data vectors. This entails repeating the other preprocessing steps. But
     * it should only be run after the experimental design is set up, the batch information has been fetched, and (of
     * course) the processed data are already available.
     */
    private void batchCorrect( ExpressionExperiment ee ) throws PreprocessingException {
        String note = "ComBat batch correction";
        String detail;

        /*
         * This leaves the raw data alone; it updates the processed data.
         */

        this.checkArrayDesign( ee );

        this.checkCorrectable( ee );

        /*
         * If there are predicted outliers, but which we decide are okay, we just go ahead.
         */
        note = "[Forced]" + note;
        detail = "Batch correction skipped outlier check.";
        PreprocessorServiceImpl.log.warn( detail );

        Collection<ProcessedExpressionDataVector> vecs = this.getProcessedExpressionDataVectors( ee );

        ExpressionDataDoubleMatrix correctedData = this.getCorrectedData( ee, vecs );

        // Convert to vectors (persist QT)
        processedExpressionDataVectorService
                .createProcessedDataVectors( ee, correctedData.toProcessedDataVectors() );

        String bConf = expressionExperimentService.getBatchConfound( ee );
        if ( bConf != null ) {
            String add = "Batch correction forced over a detected confound: " + bConf;
            //noinspection ConstantConditions // That is simply false.
            detail = ( detail == null ) ? add : detail + "\n" + add;
        }
        auditTrailService.addUpdateEvent( ee, BatchCorrectionEvent.class, note, detail );

        removeInvalidatedData( ee );
        processExceptForVectorCreate( ee );
        processBatchInfo( ee );
    }

    @Override
    @Transactional
    public void processDiagnostics( ExpressionExperiment ee ) throws PreprocessingException {
        this.processForSampleCorrelation( ee );
        this.processForMeanVarianceRelation( ee );
        this.processForPca( ee );
        // FIXME: OPT_MODE_ALL is overkill, but none of the options currently address the exact need. No big deal.
        geeqService.calculateScore( ee, GeeqService.ScoreMode.all );
    }

    private void processVectorCreate( ExpressionExperiment ee, boolean detectScaleFromData ) {
        processedExpressionDataVectorService.computeProcessedExpressionData( ee, detectScaleFromData );
    }

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
     * Do all processing steps except making the vectors (so it uses the vectors that are there) including batch
     * correction, diagnostics, and refreshing of old analyses.
     */
    private void processExceptForVectorCreate( ExpressionExperiment ee ) {
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

        processDiagnostics( ee );

        expressionExperimentService.update( ee );
        assert ee.getNumberOfDataVectors() != null;
    }

    /**
     * Create the scatter plot to evaluate heteroscedasticity.
     */
    private void processForMeanVarianceRelation( ExpressionExperiment ee ) {
        meanVarianceService.create( ee, true );
    }

    private void processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            throw new UnsupportedOperationException( "Skipping postprocessing because experiment uses "
                    + "multiple platform types. Please check valid entry and run postprocessing separately." );
        }

        ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();

        TechnologyType tt = arrayDesignUsed.getTechnologyType();
        if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
            PreprocessorServiceImpl.log
                    .info( ee + " uses a two-color array design, processing for missing values ..." );
            twoChannelMissingValueService.computeMissingValues( ee );
        }

    }

    private void processForPca( ExpressionExperiment ee ) {
        svdService.svd( ee );
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     */
    private void processForSampleCorrelation( ExpressionExperiment ee ) {
        sampleCoexpressionAnalysisService.compute( ee );
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
                .comBat( new ExpressionDataDoubleMatrix( vecs ) );

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
                .getProcessedDataVectors( ee );
        if ( vecs.isEmpty() ) {
            log.info( String.format( "No processed vectors for %s, they will be computed from raw data...", ee ) );
            return this.processedExpressionDataVectorService.computeProcessedExpressionData( ee );
        }
        processedExpressionDataVectorService.thaw( vecs );
        return vecs;
    }

    private void checkCorrectable( ExpressionExperiment ee ) throws PreprocessingException {
        boolean correctable = expressionExperimentBatchCorrectionService.checkCorrectability( ee, true );

        if ( !correctable ) {
            // throwing exception kind of annoying but not a big deal.
            throw new PreprocessingException( ee, "could not be batch-corrected (either already batch-corrected, no batch information, or invalid design)" );
        }
    }

    private void checkArrayDesign( ExpressionExperiment ee ) throws PreprocessingException {
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new PreprocessingException( ee, "Can only batch-correct data for an experiment that uses one platform; you must switch/merge first." );
        }
    }

    @SuppressWarnings("unused")
    private void checkOutliers( ExpressionExperiment ee ) throws PreprocessingException {
        Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliersByMedianCorrelation( ee );
        if ( !outliers.isEmpty() ) {
            Collection<OutlierDetails> knownOutliers = this.getAlreadyKnownOutliers( ee );
            Collection<OutlierDetails> unknownOutliers = new LinkedList<>();

            for ( OutlierDetails od : outliers ) {
                if ( !knownOutliers.contains( od ) ) {
                    unknownOutliers.add( od );
                }
            }

            if ( !unknownOutliers.isEmpty() ) {
                String newline = System.getProperty( "line.separator" );

                StringBuilder newOutliersString = new StringBuilder();

                for ( OutlierDetails od : unknownOutliers ) {
                    newOutliersString.append( od.getBioAssay().toString() ).append( newline );
                }

                throw new PreprocessingException( ee,
                        String.format( "Could not be batch-corrected because new outliers were identified. Please remove the outliers and try again.%s Newly detected outliers: %s%s",
                                newline, newline, newOutliersString ) );
            }
        }
    }
}
