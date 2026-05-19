package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.TaxonValueObject;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wraps an {@link ExpressionAnalysisResultSet} and expose it to the public API.
 */
@Getter
@Setter
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
     * When genes are included, this field is populated.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<TaxonValueObject> taxa;

    /**
     * Related analysis results.
     * <p>
     * Note that this field is excluded from the JSON serialization if left unset.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<DifferentialExpressionAnalysisResultValueObject> results;

    public DifferentialExpressionAnalysisResultSetValueObject() {

    }

    public DifferentialExpressionAnalysisResultSetValueObject( Long id ) {
        super( id );
    }

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
     * @param includeFactorValuesInContrasts include complete factorValue and secondFactorValue when serializing
     *                                       {@link ContrastResultValueObject} if true, else only the {@code factorValueId}
     *                                        and {@code secondFactorValueId} fields are populated. The latter approach
     *                                        is more compact and the full factors can be retrieved via {@link #experimentalFactors}.
     */
    public DifferentialExpressionAnalysisResultSetValueObject( ExpressionAnalysisResultSet analysisResultSet, boolean includeFactorValuesInContrasts, Map<Long, Set<Gene>> result2Genes, boolean includeTaxonInGenes ) {
        this( analysisResultSet );
        if ( !includeTaxonInGenes ) {
            // when taxon are not rendered in genes, they need to be enumerated somewhere in the payload
            this.taxa = result2Genes.values().stream()
                    .flatMap( Set::stream )
                    .map( Gene::getTaxon )
                    .distinct()
                    .map( TaxonValueObject::new )
                    .collect( Collectors.toSet() );
        }
        this.results = analysisResultSet.getResults()
                .stream()
                .map( result -> new DifferentialExpressionAnalysisResultValueObject( result, includeFactorValuesInContrasts, result2Genes.getOrDefault( result.getId(), Collections.emptySet() ), includeTaxonInGenes ) )
                .collect( Collectors.toList() );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " Id=" + getId();
    }
}
