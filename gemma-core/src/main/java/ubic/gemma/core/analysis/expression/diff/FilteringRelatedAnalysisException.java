package ubic.gemma.core.analysis.expression.diff;

import lombok.Getter;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;

/**
 * Exception raised when data filtering fails.
 *
 * @author poirigui
 */
@Getter
public class FilteringRelatedAnalysisException extends AnalysisException {

    private final DifferentialExpressionAnalysisFilterResult result;

    public FilteringRelatedAnalysisException( DifferentialExpressionAnalysisConfig config, DifferentialExpressionAnalysisFilterResult result, FilteringException e ) {
        super( config, e );
        this.result = result;
    }
}
