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

    ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee );

    /**
     * For convenience of some testing classes
     *
     * @param ee the experiment to get the batch factor for
     * @return the batch factor of the experiment, or null, if experiment has no batch factor
     */
    ExperimentalFactor getBatchFactor( ExpressionExperiment ee );

}