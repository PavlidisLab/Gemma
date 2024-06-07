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
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao;

import javax.annotation.Nullable;
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
     * @return data matrix or null if the EE has no processed EVs
     */
    @Nullable
    ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig ) throws FilteringException;

    /**
     * Provide a filtered expression data matrix.
     *
     * @param ee           the expression experiment.
     * @param filterConfig the configuration.
     * @param dataVectors  data vectors
     * @return data matrix
     */
    ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) throws FilteringException;

    ExpressionDataDoubleMatrix getFilteredMatrix( String arrayDesignName, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors ) throws FilteringException;

    /**
     * @param ee the expression experiment.
     * @return matrix of preferred data, with all missing values masked or null if there are no processed EVs
     */
    @Nullable
    ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee );

    /**
     * Obtain a raw expression data matrix for a given quantitation type
     */
    ExpressionDataDoubleMatrix getRawExpressionDataMatrix( ExpressionExperiment ee, QuantitationType quantitationType );

    DoubleMatrix<Gene, ExpressionExperiment> getRankMatrix( Collection<Gene> genes,
            Collection<ExpressionExperiment> ees, ProcessedExpressionDataVectorDao.RankMethod method );

}