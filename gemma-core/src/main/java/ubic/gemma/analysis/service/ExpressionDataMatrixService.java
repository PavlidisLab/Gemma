package ubic.gemma.analysis.service;

import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

public interface ExpressionDataMatrixService {

    /**
     * Provide a filtered expression data matrix.
     * 
     * @param ee
     * @param filterConfig
     * @return
     */
    public abstract ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig );

    /**
     * Provide a filtered expression data matrix.
     * 
     * @param ee
     * @param filterConfig
     * @param dataVectors
     * @return
     */
    public abstract ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors );

    /**
     * @param arrayDesignName
     * @param filterConfig
     * @param dataVectors
     * @return
     */
    public abstract ExpressionDataDoubleMatrix getFilteredMatrix( String arrayDesignName, FilterConfig filterConfig,
            Collection<ProcessedExpressionDataVector> dataVectors );

    /**
     * @param ee
     * @return matrix of preferred data, with all missing values masked. If the ProcessedExpressionDataVectors are
     *         missing, this will throw an exception.
     */
    public abstract ExpressionDataDoubleMatrix getProcessedExpressionDataMatrix( ExpressionExperiment ee );

    /**
     * @param ee
     * @return
     */
    public abstract Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors( ExpressionExperiment ee );

    public abstract DenseDoubleMatrix<Gene, ExpressionExperiment> getRankMatrix( Collection<Gene> genes,
            Collection<ExpressionExperiment> ees, ProcessedExpressionDataVectorDao.RankMethod method );

}