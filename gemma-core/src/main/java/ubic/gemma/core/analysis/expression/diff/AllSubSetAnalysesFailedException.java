package ubic.gemma.core.analysis.expression.diff;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Exception raised when all subset analyses failed with an {@link AnalysisException}.
 * @author poirigui
 */
@Getter
public class AllSubSetAnalysesFailedException extends AnalysisException {

    private final Collection<AnalysisException> subsetExceptions;

    public AllSubSetAnalysesFailedException( String message, Collection<AnalysisException> subsetExceptions, DifferentialExpressionAnalysisConfig config ) {
        super( message, config );
        this.subsetExceptions = new ArrayList<>( subsetExceptions );
    }
}
