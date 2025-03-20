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

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author Paul
 */
public interface SampleCoexpressionAnalysisService {

    /**
     * Loads the analysis containing the coexpression matrices for the given experiment and converts the full (non-regressed) coexpression matrix
     * into a double matrix. If the analysis or the matrix does not exist, computes it.
     *
     * @param ee the experiment to load the raw coexpression matrix for.
     * @return the full, non-regressed matrix. If the matrix is not available event after attempted computation, returns null.
     */
    @Nullable
    DoubleMatrix<BioAssay, BioAssay> loadFullMatrix( ExpressionExperiment ee );

    /**
     * Load the regressed coexpression matrix for the given experiment.
     * @return the regressed matrix if available, null might indicate that the is either no analysis or no regressed
     * matrix, use {@link #hasAnalysis(ExpressionExperiment)} to tell these apart.
     */
    @Nullable
    DoubleMatrix<BioAssay, BioAssay> loadRegressedMatrix( ExpressionExperiment ee );

    /**
     * Loads the analysis containing the coexpression matrices for the given experiment and converts the regressed coexpression matrix
     * into a double matrix. If the analysis or the matrix does not exist, computes it. If there are problems loading
     * or computing the regressed matrix (e.g. because the experiment has no experimental design), the full matrix is
     * returned instead.
     *
     * @param ee the experiment to load the regressed coexpression matrix for.
     * @return sample correlation matrix with major factors regressed out, if such matrix exists for the given experiment.
     * If not, the full non-regressed matrix is returned. If no matrix is available event after attempted computation, returns null.
     */
    @Nullable
    DoubleMatrix<BioAssay, BioAssay> loadBestMatrix( ExpressionExperiment ee );


    @Transactional(readOnly = true)
    @Nullable
    DoubleMatrix<BioAssay, BioAssay> retrieveExisting( ExpressionExperiment ee );

    @Transactional(readOnly = true)
    PreparedCoexMatrices prepare( ExpressionExperiment ee ) throws FilteringException;

    /**
     * Computes sample correlation matrices for the given experiment. If the experiment already has any, they are
     * removed. See {@link SampleCoexpressionAnalysis} for the description of the matrices that are computed.
     *
     * @param ee the experiment to create a sample correlation matrix for.
     * @return the regressed coexpression matrix if available, otherwise the full
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    DoubleMatrix<BioAssay, BioAssay> compute( ExpressionExperiment ee, PreparedCoexMatrices matrices );

    boolean hasAnalysis( ExpressionExperiment ee );

    /**
     * Removes all coexpression matrices for the given experiment.
     *
     * @param ee the experiment to remove the analysis for.
     */
    void removeForExperiment( ExpressionExperiment ee );
}

