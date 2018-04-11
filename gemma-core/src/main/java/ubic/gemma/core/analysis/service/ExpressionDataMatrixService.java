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
package ubic.gemma.core.analysis.service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao;

import java.util.Collection;

/**
 * Tools for easily getting data matrices for analysis in a consistent way.
 *
 * @author Paul
 */
public interface ExpressionDataMatrixService {

    /**
     * Provide a filtered expression data matrix.
     *
     * @param ee           the expression experiment.
     * @param filterConfig the configuration.
     * @return data matrix
     */
    ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig );

    /**
     * Provide a filtered expression data matrix.
     *
     * @param ee           the expression experiment.
     * @param filterConfig the configuration.
     * @param dataVectors  data vectors
     * @return data matrix
     */
    ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors );

    ExpressionDataDoubleMatrix getFilteredMatrix( String arrayDesignName, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors );

    /**
     * @param ee the expression experiment.
     * @return matrix of preferred data, with all missing values masked. If the ProcessedExpressionDataVectors are
     * missing, this will throw an exception.
     */
    ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee );

    DoubleMatrix<Gene, ExpressionExperiment> getRankMatrix( Collection<Gene> genes,
            Collection<ExpressionExperiment> ees, ProcessedExpressionDataVectorDao.RankMethod method );

}