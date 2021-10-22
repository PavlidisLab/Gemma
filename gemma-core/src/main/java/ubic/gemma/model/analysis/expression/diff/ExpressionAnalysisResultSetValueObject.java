package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps an {@link ExpressionAnalysisResultSet} and expose it to the public API.
 */
public class ExpressionAnalysisResultSetValueObject extends AnalysisResultSetValueObject<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> {

    private final DifferentialExpressionAnalysisValueObject analysis;
    private final Collection<ExperimentalFactorValueObject> experimentalFactors;

    /**
     * Related analysis results.
     *
     * Note that this field is excluded from the JSON serialization if left unset.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<DifferentialExpressionAnalysisResultValueObject> results;

    /**
     * Create a simple analysis results set VO with limited data.
     * @param analysisResultSet
     */
    public ExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet ) {
        super( analysisResultSet );
        this.analysis = new DifferentialExpressionAnalysisValueObject( analysisResultSet.getAnalysis() );
        this.experimentalFactors = analysisResultSet.getExperimentalFactors().stream()
                .map( ExperimentalFactorValueObject::new )
                .collect( Collectors.toList() );
    }

    /**
     * Create an expression analysis result set VO with all its associated results.
     *
     * Note: this constructor assumes that {@link ExpressionAnalysisResultSet#getResults()} has already been initialized.
     *  @param analysisResultSet
     * @param result2Gene
     */
    public ExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet, Map<DifferentialExpressionAnalysisResult, List<Gene>> result2Genes ) {
        this( analysisResultSet );
        this.results = analysisResultSet.getResults()
                .stream()
                .map( result -> new DifferentialExpressionAnalysisResultValueObject( result, result2Genes.getOrDefault( result, Collections.emptyList() ) ) )
                .collect( Collectors.toList() );
    }

    public DifferentialExpressionAnalysisValueObject getAnalysis() {
        return analysis;
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    @Override
    public Collection<DifferentialExpressionAnalysisResultValueObject> getResults() {
        return results;
    }
}
