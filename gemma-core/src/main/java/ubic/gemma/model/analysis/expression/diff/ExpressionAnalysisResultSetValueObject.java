package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.Hibernate;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Wraps an {@link ExpressionAnalysisResultSet} and expose it to the public API.
 */
public class ExpressionAnalysisResultSetValueObject extends AnalysisResultSetValueObject<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> {

    private final DifferentialExpressionAnalysisValueObject analysis;

    /**
     * Related analysis results.
     *
     * Note that this field is excluded from the JSON serialization if left unset.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<DifferentialExpressionAnalysisResultValueObject> analysisResults;

    public ExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet ) {
        super( analysisResultSet );
        this.analysis = new DifferentialExpressionAnalysisValueObject( analysisResultSet.getAnalysis() );
        if ( Hibernate.isInitialized( analysisResultSet.getResults() ) ) {
            this.analysisResults = analysisResultSet.getResults()
                    .stream().map( DifferentialExpressionAnalysisResultValueObject::new )
                    .collect( Collectors.toList() );
        }
    }

    @Override
    public DifferentialExpressionAnalysisValueObject getAnalysis() {
        return analysis;
    }

    @Override
    public Collection<DifferentialExpressionAnalysisResultValueObject> getAnalysisResults() {
        return analysisResults;
    }
}
