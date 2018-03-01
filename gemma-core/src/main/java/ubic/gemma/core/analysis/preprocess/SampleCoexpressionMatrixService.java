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
package ubic.gemma.core.analysis.preprocess;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl;

/**
 * @author Paul
 */
public interface SampleCoexpressionMatrixService {

    /**
     * Creates the matrix, regressing out major factors and removing outliers, or loads it if it already exists.
     *
     * @param ee the experiment
     * @return the coexpression matrix
     */
    DoubleMatrix<BioAssay, BioAssay> findOrCreate( ExpressionExperiment ee );

    /**
     * Creates the matrix, or loads it if it already exists.
     *
     * @param ee             the experiment
     * @param useRegression  whether to regress out major factors when creating a new matrix.
     * @param removeOutliers whether to remove marked outliers when creating a new matrix.
     * @return the coexpression matrix
     */
    DoubleMatrix<BioAssay, BioAssay> findOrCreate( ExpressionExperiment ee, boolean useRegression,
            boolean removeOutliers );

    /**
     * @param ee the experiment
     * @return true if the ee has a coexp. matrix
     */
    boolean hasMatrix( ExpressionExperiment ee );

    /**
     * @param ee the experiment to remove the matrix for
     */
    void delete( ExpressionExperiment ee );

    /**
     * Creates a sample correlation matrix for the given experiment, regressing out major factors and removing marked outliers.
     * Note that since you get a full square matrix, all correlations
     * are represented twice, and values on the main diagonal will always be 1. Method for extracting the lower triangle
     * to a linear array is here: {@link  ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl#getLowerTriangle(double[][])};
     * Also observe that the matrix can contain NaN values, as dealt with here: {@link GeeqServiceImpl#getLowerTriCormat(ubic.basecode.dataStructure.matrix.DoubleMatrix)}
     *
     * @param ee the experiment to create a sample correlation matrix for.
     * @return the sample-sample correlation matrix.
     */
    DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee );

    /**
     * Creates a sample correlation matrix for the given experiment.
     * Note that since you get a full square matrix, all correlations
     * are represented twice, and values on the main diagonal will always be 1. Method for extracting the lower triangle
     * to a linear array is here: {@link  ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl#getLowerTriangle(double[][])};
     * Also observe that the matrix can contain NaN values, as dealt with here: {@link GeeqServiceImpl#getLowerTriCormat(ubic.basecode.dataStructure.matrix.DoubleMatrix)}
     *
     * @param ee             the experiment to create a sample correlation matrix for.
     * @param useRegression  whether to regress out major factors.
     * @param removeOutliers whether to remove marked outliers.
     * @return the sample-sample correlation matrix.
     */
    DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee, boolean useRegression, boolean removeOutliers );
}