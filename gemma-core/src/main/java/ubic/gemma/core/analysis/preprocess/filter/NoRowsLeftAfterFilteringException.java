package ubic.gemma.core.analysis.preprocess.filter;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * This is a special kind of preprocessing exception that occurs when filtering the expression data matrix result in no
 * rows left.
 * @author poirigui
 */
public class NoRowsLeftAfterFilteringException extends FilteringException {

    public NoRowsLeftAfterFilteringException( ExpressionExperiment ee, String message ) {
        super( ee, message );
    }
}
