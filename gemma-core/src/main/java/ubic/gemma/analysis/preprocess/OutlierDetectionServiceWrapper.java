package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface OutlierDetectionServiceWrapper {

    /* Call default */
    public abstract Collection<OutlierDetails> findOutliers( ExpressionExperiment ee,
            OutlierDetectionService outlierDetector );

    /* Use regression and/or sort-by-median algorithm; store information about test in testDetails */
    public abstract OutlierDetectionTestDetails findOutliers( ExpressionExperiment ee,
            OutlierDetectionService outlierDetector, boolean useRegression, boolean findByMedian );

    /* Returns combined results from Raymond's algorithm and sort-by-median algorithm; always uses regression */
    public abstract OutlierDetectionTestDetails findOutliersByCombinedMethod( ExpressionExperiment ee,
            OutlierDetectionService outlierDetector );

}
