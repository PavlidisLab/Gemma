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

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean carrying the {@code processFor*} diagnostic steps that
 * {@link PreprocessorServiceImpl#processDiagnostics(ExpressionExperiment)}
 * runs as part of the post-load preprocessing pipeline.
 * <p>
 * Hoisted out of {@code PreprocessorServiceImpl} so the methods can carry
 * {@code @AuditedOnError} declarations and be intercepted by Spring AOP --
 * private self-invoked methods on {@code PreprocessorServiceImpl} were
 * invisible to the proxy and could not be migrated off the imperative
 * {@code addUpdateEvent(...)} pattern in bucket 2e.
 *
 * @see ubic.gemma.core.security.audit.AuditedOnError
 */
public interface PreprocessorHelperService {

    /**
     * If possible, batch-correct the processed data vectors via ComBat. A
     * successful run emits a {@code BatchCorrectionEvent} via the
     * {@code @Audited} aspect; the note records how many vectors were
     * replaced. When the experiment is not batch-correctable, the method
     * returns without writing a vector or an audit event.
     * <p>
     * Hoisted out of {@code PreprocessorServiceImpl} (where it lived as
     * {@code private void batchCorrect(ExpressionExperiment)} and was self-
     * invoked from {@code process()}) so the call passes through a Spring
     * proxy and the audit aspect can fire — bucket 2b of
     * {@code AUDIT_PHASE_C_RECCE.md} / inventory #3 of
     * {@code handoffs/AUDIT_RESIDUAL_INVENTORY.md}.
     *
     * @return the number of processed vectors that were replaced by the
     *         batch-corrected matrix, or {@code null} when the experiment
     *         is not batch-correctable (no audit event written in that
     *         case). The return value drives the {@code @Audited} note via
     *         {@code messageSpel}; callers may ignore it.
     */
    @Nullable
    Integer batchCorrect( ExpressionExperiment ee ) throws PreprocessingException;

    /**
     * Compute and persist the mean-variance scatter used for the
     * heteroscedasticity diagnostic plot. A failure emits a
     * {@code FailedMeanVarianceUpdateEvent} via the audit aspect.
     */
    void processForMeanVarianceRelation( ExpressionExperiment ee ) throws PreprocessingException;

    /**
     * Run SVD / PCA for diagnostics. A failure emits a
     * {@code FailedPCAAnalysisEvent} via the audit aspect.
     */
    void processForPca( ExpressionExperiment ee ) throws SVDRelatedPreprocessingException;

    /**
     * Compute and persist the sample-sample correlation matrix used for
     * the diagnostic heatmap. A failure emits a
     * {@code FailedSampleCorrelationAnalysisEvent} via the audit aspect.
     */
    void processForSampleCorrelation( ExpressionExperiment ee ) throws SampleCoexpressionRelatedPreprocessingException;
}
