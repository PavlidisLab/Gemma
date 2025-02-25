/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import org.springframework.transaction.annotation.Propagation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Encapsulates steps that are done to expression data sets after they are loaded and experimental design curated.
 * <p>
 * This can also be used to 'refresh' everything.
 * <p>
 * The following steps are performed:
 * <ol>
 * <li>Deleting old analysis files and results, as these are invalidated by the subsequent steps.S
 * <li>Computing missing values (two-channel)
 * <li>Creation of "processed" vectors
 * <li>Batch-correction</li>
 * <li>PCA
 * <li>Computing sample-wise correlation matrices for diagnostic plot
 * <li>Computing mean-variance data for diagnostic plots
 * <li>GEEQ scoring</li>
 * <li>Redoing any DEA (if this is a 'refresh')</li>
 * </ol>
 *
 * Note that since each step can be replayed and the whole process is lengthy and likely to lock parts of not whole
 * tables, it is marked as {@link Propagation#NEVER} to prevent execution within another transaction.
 *
 * @author paul
 */
public interface PreprocessorService {

    /**
     * Preprocess a dataset.
     * <p>
     * Mismatched quantitation types are ignored by default, diagnostics failure will in a {@link PreprocessingException}.
     *
     * @see #process(ExpressionExperiment, boolean, boolean)
     */
    default void process( ExpressionExperiment ee ) throws PreprocessingException {
        process( ee, true, false );
    }

    /**
     * Preprocess a dataset.
     * <p>
     * Diagnostic failure will result in a {@link PreprocessingException}.
     *
     * @see #process(ExpressionExperiment, boolean, boolean)
     */
    default void process( ExpressionExperiment ee, boolean ignoreQuantitationMismatch ) throws PreprocessingException {
        process( ee, ignoreQuantitationMismatch, false );
    }


    /**
     * Preprocess a dataset.
     *
     * @param ee                         the expression experiment to process, it must be fully thawed with {@link ExpressionExperimentService#thaw(ExpressionExperiment)}
     * @param ignoreQuantitationMismatch ignore quantitation mismatch when generating processed EVs
     * @param ignoreDiagnosticFailure    simply warn if a diagnostic fails instead of interrupting the pre-processing
     *                                   and raising an exception
     * @throws PreprocessingException if there was a problem during the processing
     */
    void process( ExpressionExperiment ee, boolean ignoreQuantitationMismatch, boolean ignoreDiagnosticFailure ) throws PreprocessingException;

    /**
     * Create or update the sample correlation, PCA and M-V data.
     * <p>
     * This is also done as part of {@link #process(ExpressionExperiment, boolean, boolean)} so should only be called if
     * only a refresh is needed.
     * @param ee the expression experiment to process, it must be fully thawed with {@link ExpressionExperimentService#thaw(ExpressionExperiment)}
     */
    void processDiagnostics( ExpressionExperiment ee ) throws PreprocessingException;
}