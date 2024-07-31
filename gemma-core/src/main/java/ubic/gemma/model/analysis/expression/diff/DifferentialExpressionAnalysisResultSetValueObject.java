package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps an {@link ExpressionAnalysisResultSet} and expose it to the public API.
 */
public class DifferentialExpressionAnalysisResultSetValueObject extends AnalysisResultSetValueObject<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> {

    private DifferentialExpressionAnalysisValueObject analysis;
    private Collection<ExperimentalFactorValueObject> experimentalFactors;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FactorValueBasicValueObject baselineGroup;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FactorValueBasicValueObject secondBaselineGroup;

    /**
     * Related analysis results.
     * <p>
     * Note that this field is excluded from the JSON serialization if left unset.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<DifferentialExpressionAnalysisResultValueObject> results;

    /**
     * Create a simple analysis results set VO with limited data.
     */
    public DifferentialExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet ) {
        super( analysisResultSet );
        this.analysis = new DifferentialExpressionAnalysisValueObject( analysisResultSet.getAnalysis() );
        // these are ignored from JSON serialization of set to null
        this.analysis.setFactorValuesUsed( null );
        this.analysis.setResultSets( null );
        this.experimentalFactors = analysisResultSet.getExperimentalFactors().stream()
                .map( ExperimentalFactorValueObject::new )
                .collect( Collectors.toList() );
        if ( analysisResultSet.getBaselineGroup() != null ) {
            this.baselineGroup = new FactorValueBasicValueObject( analysisResultSet.getBaselineGroup() );
        }
    }

    /**
     * Create an expression analysis result set VO with all its associated results.
     * <p>
     * Note: this constructor assumes that {@link ExpressionAnalysisResultSet#getResults()} has already been initialized.
     */
    public DifferentialExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet, Map<Long, List<Gene>> result2Genes ) {
        this( analysisResultSet );
        this.results = analysisResultSet.getResults()
                .stream()
                .map( result -> new DifferentialExpressionAnalysisResultValueObject( result, false, result2Genes.getOrDefault( result.getId(), Collections.emptyList() ) ) )
                .collect( Collectors.toList() );
    }

    public DifferentialExpressionAnalysisValueObject getAnalysis() {
        return analysis;
    }

    public void setAnalysis( DifferentialExpressionAnalysisValueObject analysis ) {
        this.analysis = analysis;
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactorValueObject> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    @Nullable
    public FactorValueBasicValueObject getBaselineGroup() {
        return baselineGroup;
    }

    public void setBaselineGroup( @Nullable FactorValueBasicValueObject baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    @Nullable
    @SuppressWarnings("unused")
    public FactorValueBasicValueObject getSecondBaselineGroup() {
        return secondBaselineGroup;
    }

    public void setSecondBaselineGroup( @Nullable FactorValueBasicValueObject secondBaselineGroup ) {
        this.secondBaselineGroup = secondBaselineGroup;
    }

    @Override
    public Collection<DifferentialExpressionAnalysisResultValueObject> getResults() {
        return results;
    }

    public void setResults( Collection<DifferentialExpressionAnalysisResultValueObject> results ) {
        this.results = results;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " Id=" + getId();
    }
}
