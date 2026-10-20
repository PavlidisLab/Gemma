/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchCorrectionService;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrixUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.core.security.audit.AuditedOnError;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchCorrectionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMeanVarianceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedSampleCorrelationAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link PreprocessorHelperService}: a thin co-bean that
 * exists so each {@code processFor*} method is invoked through a Spring proxy
 * and the {@link AuditedOnError} aspect can intercept its catch path.
 * <p>
 * The methods previously lived as {@code private} members of
 * {@code PreprocessorServiceImpl} and were self-invoked via {@code this.} --
 * Spring AOP cannot intercept either case, so their imperative
 * {@code auditTrailService.addUpdateEvent( ee, FailedXEvent.class, ..., e )}
 * blocks could not be migrated to {@link AuditedOnError} during bucket 2e.
 * Hoisting them onto this separately-injected bean lets the aspect see them.
 *
 * <p>{@link Propagation#NEVER} matches {@code PreprocessorServiceImpl} -- these
 * diagnostic steps each manage their own transactions internally.
 */
@Service
@Transactional(propagation = Propagation.NEVER)
public class PreprocessorHelperServiceImpl implements PreprocessorHelperService {

    private static final Log log = LogFactory.getLog( PreprocessorHelperServiceImpl.class );

    @Autowired
    private MeanVarianceService meanVarianceService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private ExpressionExperimentBatchCorrectionService expressionExperimentBatchCorrectionService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    /**
     * If possible, batch correct the processed data vectors. Returns the
     * number of vectors that were replaced — used by the {@code @Audited}
     * aspect's {@code messageSpel} to render the audit note. Returns
     * {@code null} when the experiment is not batch-correctable; the
     * {@code @AuditedConditional} predicate then skips emission.
     */
    @Override
    @AuditedConditional(value = BatchCorrectionEvent.class,
            when = "#result != null",
            messageSpel = "'ComBat batch correction, vectors were replaced with ' + #result + ' batch-corrected ones.'")
    public Integer batchCorrect( ExpressionExperiment ee ) throws PreprocessingException {
        if ( !expressionExperimentBatchCorrectionService.checkCorrectability( ee ) ) {
            log.warn( ee + " is not batch-correctable, will not perform ComBat." );
            return null;
        }

        Collection<ProcessedExpressionDataVector> vecs;
        try {
            vecs = getProcessedExpressionDataVectors( ee );
        } catch ( QuantitationTypeConversionException e ) {
            throw new QuantitationTypeConversionRelatedPreprocessingException( ee, e );
        }

        List<ProcessedExpressionDataVector> correctedVectors = getCorrectedData( ee, vecs );

        QuantitationType correctedQt = correctedVectors.iterator().next().getQuantitationType();

        // ComBat will create a new QT, but will not pass on the preferred flag
        correctedQt.setIsMaskedPreferred( true );

        // Convert to vectors (persist QT)
        return processedExpressionDataVectorService.replaceProcessedDataVectors( ee, correctedVectors, false );
    }

    private List<ProcessedExpressionDataVector> getCorrectedData( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vecs ) throws PreprocessingException {
        ExpressionDataDoubleMatrix correctedData = expressionExperimentBatchCorrectionService
                .comBat( ee, new ExpressionDataDoubleMatrix( ee, vecs ) );

        if ( correctedData == null ) {
            throw new PreprocessingException( ee, "could not be batch-corrected: ComBat did not found a suitable batch factor" );
        }

        List<ProcessedExpressionDataVector> correctedVectors = BulkExpressionDataMatrixUtils.toVectors( correctedData, ProcessedExpressionDataVector.class );

        if ( correctedVectors.size() != vecs.size() ) {
            throw new PreprocessingException( ee, "could not be batch-corrected: matrix returned by ComBat had wrong number of rows" );
        }

        QuantitationType batchCorrectedQt = correctedVectors.iterator().next().getQuantitationType();
        if ( !batchCorrectedQt.getIsBatchCorrected() ) {
            throw new IllegalStateException( "Batch correction did not set the isBatchCorrected flag on " + batchCorrectedQt + "." );
        }

        return correctedVectors;
    }

    private Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors( ExpressionExperiment ee ) throws QuantitationTypeConversionException {
        Collection<ProcessedExpressionDataVector> vecs = processedExpressionDataVectorService
                .getProcessedDataVectorsAndThaw( ee );
        if ( vecs.isEmpty() ) {
            log.info( String.format( "No processed vectors for %s, they will be computed from raw data...", ee ) );
            processedExpressionDataVectorService.createProcessedDataVectors( ee, true );
            vecs = processedExpressionDataVectorService.getProcessedDataVectorsAndThaw( ee );
        }
        return vecs;
    }

    /**
     * Create the scatter plot to evaluate heteroscedasticity.
     */
    @Override
    @AuditedOnError(FailedMeanVarianceUpdateEvent.class)
    public void processForMeanVarianceRelation( ExpressionExperiment ee ) throws PreprocessingException {
        try {
            meanVarianceService.create( ee, true );
        } catch ( Exception e ) {
            throw new PreprocessingException( ee, e );
        }
    }

    @Override
    @AuditedOnError(FailedPCAAnalysisEvent.class)
    public void processForPca( ExpressionExperiment ee ) throws SVDRelatedPreprocessingException {
        try {
            svdService.svd( ee );
        } catch ( SVDException e ) {
            throw new SVDRelatedPreprocessingException( ee, e );
        }
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     */
    @Override
    @AuditedOnError(FailedSampleCorrelationAnalysisEvent.class)
    public void processForSampleCorrelation( ExpressionExperiment ee ) throws SampleCoexpressionRelatedPreprocessingException {
        try {
            sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
        } catch ( FilteringException e ) {
            throw new FilteringRelatedPreprocessingException( ee, e );
        } catch ( Exception e ) {
            throw new SampleCoexpressionRelatedPreprocessingException( ee, e );
        }
    }
}
