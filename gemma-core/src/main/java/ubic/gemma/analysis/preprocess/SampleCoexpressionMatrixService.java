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
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 * @version $Id$
 */
public interface SampleCoexpressionMatrixService {

    /**
     * Creates the matrix, or loads it if it already exists.
     * 
     * @param expressionExperiment
     */
    public abstract DoubleMatrix<BioAssay, BioAssay> findOrCreate( ExpressionExperiment expressionExperiment );

    /**
     * Retrieve (and if necessary compute) the correlation matrix for the samples.
     * 
     * @param ee
     * @return Matrix, sorted by experimental design
     */
    public abstract DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee, boolean forceRecompute );

    /**
     * @param processedVectors
     * @return correlation matrix. The matrix is NOT sorted by the experimental design.
     */
    public abstract DoubleMatrix<BioAssay, BioAssay> create( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> processedVectors );

    public abstract boolean hasMatrix( ExpressionExperiment ee );

}