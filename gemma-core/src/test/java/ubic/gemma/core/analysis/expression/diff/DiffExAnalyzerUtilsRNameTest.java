package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the R-friendly name helpers in {@link DiffExAnalyzerUtils} +
 * the interaction-term formatter. These helpers are consumed by the
 * differential-expression pipeline when it ships a design matrix off to R; the
 * naming contract (prefix + id, baseline suffix) is what lets us round-trip
 * coefficient names back to factor / factor-value objects on the Java side.
 *
 * @author claude
 */
public class DiffExAnalyzerUtilsRNameTest {

    @Test
    public void nameForR_bioMaterial_prependsBiomatPrefix() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setId( 42L );
        assertThat( DiffExAnalyzerUtils.nameForR( bm ) ).isEqualTo( "biomat_42" );
    }

    @Test
    public void nameForR_bioMaterial_withNullId_throws() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        assertThatThrownBy( () -> DiffExAnalyzerUtils.nameForR( bm ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Sample must have an ID" );
    }

    @Test
    public void nameForR_experimentalFactor_prependsFactPrefix() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        ef.setId( 1001L );
        assertThat( DiffExAnalyzerUtils.nameForR( ef ) ).isEqualTo( "fact.1001" );
    }

    @Test
    public void nameForR_experimentalFactor_withNullId_throws() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        assertThatThrownBy( () -> DiffExAnalyzerUtils.nameForR( ef ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Factor must have an ID" );
    }

    @Test
    public void nameForR_factorValue_categoricalNonBaseline_prependsFvPrefix() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setId( 5005L );
        assertThat( DiffExAnalyzerUtils.nameForR( fv, false ) ).isEqualTo( "fv_5005" );
    }

    @Test
    public void nameForR_factorValue_categoricalBaseline_appendsBaselineSuffix() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setId( 5005L );
        assertThat( DiffExAnalyzerUtils.nameForR( fv, true ) ).isEqualTo( "fv_5005_base" );
    }

    @Test
    public void nameForR_factorValue_continuousMarkedAsBaseline_throws() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "age", FactorType.CONTINUOUS );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setId( 7007L );
        assertThatThrownBy( () -> DiffExAnalyzerUtils.nameForR( fv, true ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Continuous factors cannot have a baseline" );
    }

    @Test
    public void nameForR_factorValue_continuousNotBaseline_works() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "age", FactorType.CONTINUOUS );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setId( 7007L );
        assertThat( DiffExAnalyzerUtils.nameForR( fv, false ) ).isEqualTo( "fv_7007" );
    }

    @Test
    public void nameForR_factorValue_withNullId_throws() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        assertThatThrownBy( () -> DiffExAnalyzerUtils.nameForR( fv, false ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Factor value must have an ID" );
    }

    @Test
    public void nameForR_compositeSequence_returnsStringifiedId() {
        CompositeSequence cs = CompositeSequence.Factory.newInstance();
        cs.setId( 9001L );
        assertThat( DiffExAnalyzerUtils.nameForR( cs ) ).isEqualTo( "9001" );
    }

    @Test
    public void nameForR_compositeSequence_withNullId_throws() {
        CompositeSequence cs = CompositeSequence.Factory.newInstance();
        assertThatThrownBy( () -> DiffExAnalyzerUtils.nameForR( cs ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Design element must be persistent" );
    }

    @Test
    public void formatInteraction_joinsFactorNamesByColon_sortedDeterministic() {
        // ExperimentalFactor.COMPARATOR sorts by name (then id). Construct two factors
        // and verify the result is the colon-joined sorted-name string.
        ExperimentalFactor a = ExperimentalFactor.Factory.newInstance( "alpha", FactorType.CATEGORICAL );
        a.setId( 1L );
        ExperimentalFactor b = ExperimentalFactor.Factory.newInstance( "beta", FactorType.CATEGORICAL );
        b.setId( 2L );

        String result = DiffExAnalyzerUtils.formatInteraction( Set.of( a, b ) );
        // Whatever the comparator's tiebreaker, the joiner must produce one of these
        // two deterministic orderings — the contract is sortedness, not a specific order.
        assertThat( result ).isIn( "alpha:beta", "beta:alpha" );
        // And the single result is consistent across calls.
        assertThat( DiffExAnalyzerUtils.formatInteraction( Set.of( a, b ) ) ).isEqualTo( result );
    }

    @Test
    public void formatInteraction_singleFactor_returnsBareFactorName() {
        ExperimentalFactor a = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        a.setId( 1L );
        assertThat( DiffExAnalyzerUtils.formatInteraction( Set.of( a ) ) ).isEqualTo( "treatment" );
    }
}
