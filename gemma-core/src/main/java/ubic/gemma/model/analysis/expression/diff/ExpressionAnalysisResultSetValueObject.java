package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.analysis.AnalysisResultValueObject;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Wraps an {@link ExpressionAnalysisResultSet} and expose it to the public API.
 */
public class ExpressionAnalysisResultSetValueObject extends AnalysisResultSetValueObject<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> {

    // FIXME: use Collection<DifferentialExpressionAnalysisResultValueObject>
    private final Collection analysisResults;

    public ExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet ) {
        super( analysisResultSet );
        this.analysisResults = analysisResultSet.getResults()
                .stream().map( DifferentialExpressionAnalysisResultValueObject::new )
                .collect( Collectors.toList() );
    }

    @Override
    public Collection<AnalysisResultValueObject<DifferentialExpressionAnalysisResult>> getAnalysisResults() {
        return this.analysisResults;
    }
}
