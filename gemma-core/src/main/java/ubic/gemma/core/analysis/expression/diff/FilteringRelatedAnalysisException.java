package ubic.gemma.core.analysis.expression.diff;

import ubic.gemma.core.analysis.preprocess.filter.FilteringException;

/**
 * Exception raised when data filtering fails.
 * @author poirigui
 */
public class FilteringRelatedAnalysisException extends AnalysisException {

    public FilteringRelatedAnalysisException( DifferentialExpressionAnalysisConfig config, FilteringException e ) {
        super( config, e );
    }
}
