package ubic.gemma.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface BatchInfoPopulationService {

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     * 
     * @param ee
     * @return true if information was successfully obtained
     */
    public abstract boolean fillBatchInformation( ExpressionExperiment ee );

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     * 
     * @param ee
     * @param force
     * @return true if information was successfully obtained
     */
    public abstract boolean fillBatchInformation( ExpressionExperiment ee, boolean force );

}