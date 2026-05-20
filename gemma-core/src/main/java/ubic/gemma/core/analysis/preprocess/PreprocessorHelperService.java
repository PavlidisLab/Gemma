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
