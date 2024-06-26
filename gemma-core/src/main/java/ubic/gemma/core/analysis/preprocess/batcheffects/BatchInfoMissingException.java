package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Indicate that batch information is missing.
 */
public class BatchInfoMissingException extends BatchInfoPopulationException {

    public BatchInfoMissingException( ExpressionExperiment ee, String message ) {
        super( ee, message );
    }

    public BatchInfoMissingException( ExpressionExperiment ee, String message, Throwable cause ) {
        super( ee, message, cause );
    }
}
