package ubic.gemma.core.analysis.preprocess.filter;

import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author poirigui
 */
public class FilteringException extends PreprocessingException {

    public FilteringException( ExpressionExperiment ee, String message ) {
        super( ee, message );
    }
}
