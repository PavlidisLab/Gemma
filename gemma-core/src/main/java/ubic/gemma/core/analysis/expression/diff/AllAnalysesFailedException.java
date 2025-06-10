package ubic.gemma.core.analysis.expression.diff;

import java.util.Collection;

/**
 * Exception raised when all analyses fail.
 * <p>
 * If analysis are related to a particular subset factor, use {@link AllSubSetAnalysesFailedException} instead.
 * @author poirigui
 */
public class AllAnalysesFailedException extends AnalysisException {

    public AllAnalysesFailedException( String message, Collection<? extends AnalysisException> analysisExceptions, DifferentialExpressionAnalysisConfig config ) {
        super( message, config );
        analysisExceptions.forEach( this::addSuppressed );
    }
}
