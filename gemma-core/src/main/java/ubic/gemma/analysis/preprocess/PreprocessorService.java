package ubic.gemma.analysis.preprocess;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface PreprocessorService {

    /**
     * @param ee
     */
    // private void process( ExpressionExperiment ee ) {
    //
    // processForMissingValues( ee );
    //
    // /*
    // * Normalize here?
    // */
    //

    //
    // batchInfoPopulationService.fillBatchInformation( ee );
    // svdService.svd( ee );
    //
    // /*
    // * Batch correct here.
    // */
    //
    // sampleCoexpressionMatrixService.getSampleCorrelationMatrix( ee );
    // }

    public abstract void createProcessedVectors( ExpressionExperiment ee );

    /**
     * @param ee
     */
    public abstract void process( ExpressionExperiment ee );

}