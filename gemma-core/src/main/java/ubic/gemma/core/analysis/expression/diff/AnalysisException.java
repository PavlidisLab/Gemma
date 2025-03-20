package ubic.gemma.core.analysis.expression.diff;

import lombok.Getter;

/**
 * Base exception for all differential analysis-related exceptions.
 * @author poirigui
 */
@Getter
public class AnalysisException extends RuntimeException {

    private final DifferentialExpressionAnalysisConfig config;

    public AnalysisException( String message, DifferentialExpressionAnalysisConfig config ) {
        super( message );
        this.config = config;
    }

    public AnalysisException( Throwable cause, DifferentialExpressionAnalysisConfig config ) {
        super( cause );
        this.config = config;
    }
}
