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
package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 */
public interface SampleCoexpressionAnalysisService {

    /**
     * Loads the analysis containing the coexpression matrices for the given experiment and converts the raw coexpression matrix
     * into a double matrix. If the analysis does not exist, computes it.
     *
     * @param ee the experiment to load the raw coexpression matrix for.
     */
    DoubleMatrix<BioAssay, BioAssay> loadRawMatrix( ExpressionExperiment ee );

    /**
     * Loads the analysis containing the coexpression matrices for the given experiment and converts the regressed coexpression matrix
     * into a double matrix. If the analysis does not exist, computes it.
     *
     * @param ee the experiment to load the regressed coexpression matrix for.
     */
    DoubleMatrix<BioAssay, BioAssay> loadRegressedMatrix( ExpressionExperiment ee );

    /**
     * Loads the analysis containing the coexpression matrices for the given experiment. If the analysis does not
     * exist, computes it.
     *
     * @param ee the experiment to load the analysis for.
     */
    SampleCoexpressionAnalysis load( ExpressionExperiment ee );

    /**
     * @param ee the experiment.
     * @return true if the ee has the coexpression matrices computed.
     */
    boolean hasAnalysis( ExpressionExperiment ee );

    /**
     * Computes sample correlation matrices for the given experiment. If the experiment already has any, they are
     * removed. See {@link SampleCoexpressionAnalysis} for the description of the matrices that are computed.
     *
     * @param ee the experiment to create a sample correlation matrix for.
     * @return the analysis object containing several types of sample correlation matrices.
     */
    SampleCoexpressionAnalysis compute( ExpressionExperiment ee );

    /**
     * Removes all coexpression matrices for the given experiment.
     *
     * @param ee the experiment to remove the analysis for.
     */
    void removeForExperiment( ExpressionExperiment ee );

}