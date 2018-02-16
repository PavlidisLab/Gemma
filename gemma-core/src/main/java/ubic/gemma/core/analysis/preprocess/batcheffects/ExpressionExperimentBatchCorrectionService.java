/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 */
public interface ExpressionExperimentBatchCorrectionService {

    void checkBatchEffectSeverity( ExpressionExperiment ee );

    /**
     * Has it already been batch corrected? Is there a Batch factor provided? Is there a confound problem? Do we have at
     * least two samples per batch?
     *
     * @param ee    the experiment
     * @return whether it is correctable
     */
    boolean checkCorrectability( ExpressionExperiment ee );

    /**
     * Has it already been batch corrected? Is there a Batch factor provided? Is there a confound problem? Do we have at
     * least two samples per batch?
     *
     * @param ee    the experiment
     * @param force whether the correctability should ignore detected batch confound
     * @return whether it is correctable
     */
    boolean checkCorrectability( ExpressionExperiment ee, boolean force );

    /**
     * Run ComBat using default settings (parametric)
     *
     * @param mat the matrix
     * @return batch corrected matrix
     */
    ExpressionDataDoubleMatrix comBat( ExpressionDataDoubleMatrix mat );

    /**
     * @param parametric          if false, the non-parametric (slow) ComBat estimation will be used.
     * @param importanceThreshold a p-value threshold used to select covariates. Covariates which are not associated
     *                            with one of the first three principal components of the data at this level of significance will be removed
     *                            from the correction model fitting.
     * @param originalDataMatrix  the original matrix
     * @return corrected data.
     */
    ExpressionDataDoubleMatrix comBat( ExpressionDataDoubleMatrix originalDataMatrix, boolean parametric,
            Double importanceThreshold );

    ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee );

    ExperimentalFactor getBatchFactor( ExpressionExperiment ee );

}