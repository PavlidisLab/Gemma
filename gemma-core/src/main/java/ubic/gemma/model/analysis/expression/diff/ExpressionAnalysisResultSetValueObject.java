package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.analysis.AnalysisResultValueObject;
import ubic.gemma.model.analysis.AnalysisValueObject;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Wraps an {@link ExpressionAnalysisResultSet} and expose it to the public API.
 */
public class ExpressionAnalysisResultSetValueObject extends AnalysisResultSetValueObject<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> {

    private final DifferentialExpressionAnalysisValueObject analysis;

    private final Collection<AnalysisResultValueObject<DifferentialExpressionAnalysisResult>> analysisResults;

    public ExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet ) {
        super( analysisResultSet );
        this.analysis = new DifferentialExpressionAnalysisValueObject( analysisResultSet.getAnalysis() );
        this.analysisResults = analysisResultSet.getResults()
                .stream().map( DifferentialExpressionAnalysisResultValueObject::new )
                .collect( Collectors.toList() );
    }

    @Override
    public AnalysisValueObject getAnalysis() {
        return analysis;
    }

    @Override
    public Collection<AnalysisResultValueObject<DifferentialExpressionAnalysisResult>> getAnalysisResults() {
        return analysisResults;
    }
}
