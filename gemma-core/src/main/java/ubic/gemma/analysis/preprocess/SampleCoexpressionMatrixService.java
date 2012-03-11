package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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