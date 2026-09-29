package ubic.gemma.core.loader.expression;

import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Raised by {@link DataUpdater} when new raw data was stored for an experiment, but post-processing it failed.
 * <p>
 * The raw data change is committed and stays. The processed data, PCA, sample correlation matrix and differential
 * expression analyses are missing or were computed from the previous raw data, and must be regenerated.
 */
public class RawDataPostprocessingException extends PreprocessingException {

    public RawDataPostprocessingException( ExpressionExperiment ee, String rawDataChange, Throwable cause ) {
        super( ee, String.format( "The raw data was %s, but post-processing it failed. The processed data, PCA, "
                + "sample correlation matrix and differential expression analyses must be regenerated "
                + "(e.g. with makeProcessedData).", rawDataChange ), cause );
    }
}
