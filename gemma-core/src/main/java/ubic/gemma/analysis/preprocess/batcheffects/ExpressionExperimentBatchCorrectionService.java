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
package ubic.gemma.analysis.preprocess.batcheffects;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 * @version $Id$
 */
public interface ExpressionExperimentBatchCorrectionService {

    /**
     * @param ee
     */
    public abstract void checkBatchEffectSeverity( ExpressionExperiment ee );

    /**
     * Has it already been batch corrected? Is there a Batch factor provided? Is there a confound problem? Do we have at
     * least two samples per batch?
     * 
     * @param ee
     */
    public abstract boolean checkCorrectability( ExpressionExperiment ee );

    /**
     * Run ComBat using default settings (parametric)
     * 
     * @param ee
     * @param mat
     * @return
     */
    public abstract ExpressionDataDoubleMatrix comBat( ExpressionDataDoubleMatrix mat );

    /**
     * @param ee
     * @param originalDataMatrix
     * @param parametric if false, the non-parametric (slow) ComBat estimation will be used.
     * @param importanceThreshold a p-value threshold used to select covariates. Covariates which are not associated
     *        with one of the first three principal components of the data at this level of significance will be removed
     *        from the correction model fitting.
     * @return corrected data.
     */
    public abstract ExpressionDataDoubleMatrix comBat( ExpressionDataDoubleMatrix originalDataMatrix,
            boolean parametric, Double importanceThreshold );

    /**
     * @param ee
     * @return
     */
    public abstract ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee );

    /**
     * @param ee
     * @return
     */
    public abstract ExperimentalFactor getBatchFactor( ExpressionExperiment ee );

}