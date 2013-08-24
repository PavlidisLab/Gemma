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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public interface OutlierDetectionService {

    /**
     * Use default settings.
     * 
     * @param ee
     * @return
     */
    public abstract Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee );

    /* Jenni's code for outlier detection validation */
    public abstract OutlierDetectionTestDetails identifyOutliers( ExpressionExperiment ee, boolean useRegression,
            boolean findByMedian );

    /* Jenni's code for detecting outliers by combining two detection methods */
    public abstract OutlierDetectionTestDetails identifyOutliersByCombinedMethod( ExpressionExperiment ee );

    /**
     * @param ee
     * @param useRegression whether the experimental design should be accounted for (Based on our tests, it tends to
     *        cause more outlier calls, not fewer)
     * @param which quantile the correlation has to be in before it's considered potentially outlying (suggestion: 15)
     * @param what fraction of samples have to have a correlation lower than the quantile for a sample, for that sample
     *        to be considered an outlier (suggestion: 0.9)
     * @return
     */
    public abstract Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee, boolean useRegression,
            int quantileThreshold, double fractionThreshold );

    public abstract Collection<OutlierDetails> identifyOutliersByMedianCorrelation( ExpressionExperiment ee,
            boolean useRegression );

    /**
     * @param ee
     * @param cormat pre-computed correlation matrix.
     * @param quantileThreshold which quantile the correlation has to be in before it's considered potentially outlying
     *        (suggestion: 15)
     * @param fractionThreshold what fraction of samples have to have a correlation lower than the quantile for a
     *        sample, for that sample to be considered an outlier (suggestion: 0.9)
     * @return
     */
    public abstract Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee,
            DoubleMatrix<BioAssay, BioAssay> cormat, int quantileThreshold, double fractionThreshold );

}