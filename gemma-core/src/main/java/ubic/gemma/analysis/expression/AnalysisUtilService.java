package ubic.gemma.analysis.expression;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface AnalysisUtilService {

    /**
     * Remove all analyses for the experiment (Differential, Coexpression and PCA). Call this when something has
     * happened to the data to invalidate them.
     * 
     * @param expExp
     * @return true if all the analyses were deleted, false if there are associations (or other exceptional
     *         circumastances) which block any of the deletions.
     */
    public abstract boolean deleteOldAnalyses( ExpressionExperiment expExp );

}