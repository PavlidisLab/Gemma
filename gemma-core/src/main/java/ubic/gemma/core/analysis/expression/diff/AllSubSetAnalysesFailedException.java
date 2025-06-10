package ubic.gemma.core.analysis.expression.diff;

import lombok.Getter;

import java.util.Collection;

/**
 * Exception raised when all subset analyses failed with an {@link AnalysisException}.
 * @author poirigui
 */
@Getter
public class AllSubSetAnalysesFailedException extends AllAnalysesFailedException {

    public AllSubSetAnalysesFailedException( String message, Collection<? extends AnalysisException> subsetExceptions, DifferentialExpressionAnalysisConfig config ) {
        super( message, subsetExceptions, config );
    }
}
