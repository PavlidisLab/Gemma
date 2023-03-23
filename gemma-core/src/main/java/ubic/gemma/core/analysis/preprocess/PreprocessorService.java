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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 */
public interface PreprocessorService {

    /**
     * Preprocess a dataset.
     * @param ee the expression experiment to process
     */
    void process( ExpressionExperiment ee ) throws PreprocessingException;

    /**
     * Preprocess a dataset.
     * @param ee                         the expression experiment to process
     * @param ignoreQuantitationMismatch ignore quantitation mismatch when generating processed EVs
     * @throws PreprocessingException if there was a problem during the processing
     */
    void process( ExpressionExperiment ee, boolean ignoreQuantitationMismatch ) throws PreprocessingException;

    /**
     * A lightweight flavour of {@link #process(ExpressionExperiment, boolean)}.
     * <p>
     * The following are skipped: two-channel missing values; redoing differential expression; batch correction.
     */
    void processLight( ExpressionExperiment ee ) throws PreprocessingException;

    /**
     * Create or update the sample correlation, PCA and M-V data. This is also done as part of process so should only be
     * called if only a refresh is needed.
     */
    void processDiagnostics( ExpressionExperiment ee ) throws PreprocessingException;
}