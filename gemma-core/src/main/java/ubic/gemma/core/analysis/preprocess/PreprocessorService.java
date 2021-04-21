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

    ExpressionExperiment process( ExpressionExperiment ee ) throws PreprocessingException;

    /**
     * @param  ee                     the experiment
     * @param  light                  if true, just do the bare minimum. The following are skipped: two-channel missing
     *                                values; redoing
     *                                differential expression; batch correction.
     * @throws PreprocessingException if there was a problem during the processing
     */
    void process( ExpressionExperiment ee, boolean light ) throws PreprocessingException;

    /**
     * If possible, batch correct the processed data vectors. This entails repeating the other preprocessing steps. But
     * it should only be run after the experimental design is set up, the batch information has been fetched, and (of
     * course) the processed data are already available.
     *
     * @param  ee                     to be processed
     * @param  allowOutliers          whether the computationally predicted outliers should stand in the way of batch
     *                                correction.
     *                                Set to true to ignore outlier checks. If you have already removed/evaluated
     *                                outliers then setting this to
     *                                true is safe.
     * @throws PreprocessingException if there was a problem during the batch correction
     */
    void batchCorrect( ExpressionExperiment ee, boolean allowOutliers ) throws PreprocessingException;

    /**
     * Create or update the sample correlation, PCA and M-V data. This is also done as part of process so should only be
     * called if only a refresh is needed.
     * 
     * @param ee to be processed
     */
    void processDiagnostics( ExpressionExperiment ee );

}