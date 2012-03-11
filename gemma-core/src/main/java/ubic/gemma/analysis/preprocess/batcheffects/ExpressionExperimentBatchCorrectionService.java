package ubic.gemma.analysis.preprocess.batcheffects;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface ExpressionExperimentBatchCorrectionService {

    /**
     * @param ee
     */
    public abstract void checkBatchEffectSeverity( ExpressionExperiment ee );

    /**
     * Is there a confound problem? Do we have at least two samples per batch?
     * 
     * @param ee
     */
    public abstract boolean checkCorrectability( ExpressionExperiment ee );

    /**
     * @param ee
     * @return
     */
    public abstract ExperimentalFactor getBatchFactor( ExpressionExperiment ee );

    /**
     * @param ee
     * @return
     */
    public abstract ExpressionDataDoubleMatrix comBat( ExpressionExperiment ee );

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

}