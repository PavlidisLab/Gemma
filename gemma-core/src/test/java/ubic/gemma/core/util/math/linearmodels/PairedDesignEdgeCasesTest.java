package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * Edge cases of a blocking factor in the design. The golden agreement with limma is
 * {@link PairedDesignFitTest}; these pin the shapes that limma would also handle but that break
 * implementations that assume a balanced two-way grid.
 */
public class PairedDesignEdgeCasesTest {

    /**
     * An incomplete pair (one arm missing) is still fittable by OLS -- the missing arm's subject level simply
     * adjusts that subject's intercept. limma lmFit handles it; Gemma must too. This is an improvement over the
     * old all-or-nothing block-completeness gate, which could not have run this design at all.
     * <p>
     * Data: 5 samples; subjects s1, s2 both arms, s3 ctrl only. Model ~ subject + treatment, 4 coefficients
     * (intercept, s2, s3, treat), residual dof = 1.
     */
    @Test
    public void unbalancedPairsAreFittable() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 5, 2 );
        design.setColumnNames( Arrays.asList( "subject", "treatment" ) );
        design.setRowNames( Arrays.asList( "a", "b", "c", "d", "e" ) );
        // rows: s1/ctrl, s1/treat, s2/ctrl, s2/treat, s3/ctrl
        String[] subj = { "s1", "s1", "s2", "s2", "s3" };
        String[] trt = { "ctrl", "treat", "ctrl", "treat", "ctrl" };
        for ( int i = 0; i < 5; i++ ) {
            design.set( i, 0, subj[i] );
            design.set( i, 1, trt[i] );
        }

        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setBaseline( "subject", "s1" );
        dm.setBaseline( "treatment", "ctrl" );

        DoubleMatrix2D A = dm.getDoubleMatrix();
        assertThat( A.rows() ).isEqualTo( 5 );
        assertThat( A.columns() ).isEqualTo( 4 ); // intercept + s2 + s3 + treat

        DenseDoubleMatrix2D data = new DenseDoubleMatrix2D( new double[][] { { 10, 12, 14, 18, 30 } } );
        LeastSquaresFit fit = new LeastSquaresFit( dm, new NamedSingleRowData( data, "a", "b", "c", "d", "e" ) );
        assertThat( fit.getResidualDof() ).isEqualTo( 1 );

        // treatment effect = mean of within-subject differences = (2 + 4)/2 = 3. Summaries are keyed by the
        // DATA row name, not the first column name.
        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_0" );
        assertThat( s.getContrastCoefficients( "treatment" ).get( "treatmenttreat" ) )
                .isCloseTo( 3.0, offset( 1e-9 ) );
    }

    /**
     * All-singleton subjects (every subject has exactly one arm) make the treatment effect unidentifiable:
     * each single-arm subject indicator spans the rows of its arm, so subject columns are collinear with
     * intercept + treatment. The fit must not throw -- rank-deficiency is handled downstream -- but the
     * residual dof must make the degeneracy visible (n - rank <= 0 territory).
     */
    @Test
    public void allSingletonSubjectsAreRankDeficient() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 4, 2 );
        design.setColumnNames( Arrays.asList( "subject", "treatment" ) );
        design.setRowNames( Arrays.asList( "a", "b", "c", "d" ) );
        String[] subj = { "s1", "s2", "s3", "s4" };
        String[] trt = { "ctrl", "ctrl", "treat", "treat" };
        for ( int i = 0; i < 4; i++ ) {
            design.set( i, 0, subj[i] );
            design.set( i, 1, trt[i] );
        }

        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setBaseline( "subject", "s1" );
        dm.setBaseline( "treatment", "ctrl" );

        // columns: intercept, s2, s3, s4, treat = 5 columns over 4 samples. LeastSquaresFit refuses the fit
        // outright (same guard the missing-value path applies): a design with no residual dof cannot be run,
        // and the alternative was a colt ArrayIndexOutOfBoundsException from inside the QR.
        DenseDoubleMatrix2D data = new DenseDoubleMatrix2D( new double[][] { { 10, 11, 20, 21 } } );
        assertThatThrownBy( () -> new LeastSquaresFit( dm, new NamedSingleRowData( data, "a", "b", "c", "d" ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "No residual degrees of freedom" );
    }

    /**
     * A CONTINUOUS factor cannot be a blocking factor: it has one column and no subject levels to block on.
     * (A one-level categorical factor is caught upstream by the constant-factor logic; the config level only
     * rejects what the config alone can see.)
     */
    @Test
    public void continuousFactorIsRejectedAsBlocking() {
        ExperimentalFactor continuous = ExperimentalFactor.Factory.newInstance();
        continuous.setName( "age" );
        continuous.setType( FactorType.CONTINUOUS );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        assertThatThrownBy( () -> config.setBlockingFactors( Collections.singletonList( continuous ) ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    /**
     * Blocking factors must survive the config copy constructor, because subsetting copies the config per
     * subset arm (fixConfigForSubset) -- a paired + subsetted analysis loses its pairing otherwise.
     */
    @Test
    public void blockingFactorsSurviveConfigCopy() {
        ExperimentalFactor subject = ExperimentalFactor.Factory.newInstance();
        subject.setName( "subject" );
        subject.setType( FactorType.CATEGORICAL );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setBlockingFactors( Collections.singletonList( subject ) );

        DifferentialExpressionAnalysisConfig copy = new DifferentialExpressionAnalysisConfig( config );
        assertThat( copy.getBlockingFactors() ).containsExactly( subject );
    }

    /**
     * A blocking factor whose subject levels are USELESS (one level in the data) is just a constant column;
     * the analyzer's constant-factor handling governs, and flagging it must not crash the config.
     */
    @Test
    public void oneLevelCategoricalFactorIsAcceptedByConfig() {
        ExperimentalFactor subject = ExperimentalFactor.Factory.newInstance();
        subject.setName( "subject" );
        subject.setType( FactorType.CATEGORICAL );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setBlockingFactors( Collections.singletonList( subject ) );
        assertThat( config.getBlockingFactors() ).containsExactly( subject );
    }

    /**
     * {@link LeastSquaresFit} reads row names from the data matrix, and {@link #unbalancedPairsAreFittable}
     * looks the summary up by probe key -- so the single row of data needs a name. The double matrix type
     * under test here carries row names through the fit.
     */
    private static class NamedSingleRowData extends DenseDoubleMatrix<String, String> {
        NamedSingleRowData( DoubleMatrix2D values, String... colNames ) {
            super( values.toArray() );
            setColumnNames( Arrays.asList( colNames ) );
            setRowNames( Collections.singletonList( "probe_0" ) );
        }
    }

    private static ExperimentalFactor categorical( String name ) {
        ExperimentalFactor f = ExperimentalFactor.Factory.newInstance();
        f.setName( name );
        f.setType( FactorType.CATEGORICAL );
        return f;
    }

    /**
     * The copy constructor (used for the subset arm and for redo) carries the mixed-model flag too: a copy
     * that dropped it would refit a mixed analysis as fixed-effect.
     */
    @Test
    public void estimateBlockingCorrelationSurvivesConfigCopy() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        assertThat( config.isEstimateBlockingCorrelation() ).isFalse();
        config.setEstimateBlockingCorrelation( true );
        assertThat( new DifferentialExpressionAnalysisConfig( config ).isEstimateBlockingCorrelation() ).isTrue();
    }

    /**
     * setBlockingFactors replaces; it does not accumulate, so a redo that recovers the old set cannot leave
     * a stale factor from an earlier call behind.
     */
    @Test
    public void settingBlockingFactorsReplacesThePreviousSet() {
        ExperimentalFactor a = categorical( "a" ), b = categorical( "b" );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setBlockingFactors( Collections.singletonList( a ) );
        config.setBlockingFactors( Collections.singletonList( b ) );
        assertThat( config.getBlockingFactors() ).containsExactly( b );
        config.setBlockingFactors( Collections.emptyList() );
        assertThat( config.getBlockingFactors() ).isEmpty();
    }

    /**
     * An interaction is refused when either member is a blocking factor, whichever side it sits on.
     */
    @Test
    public void interactionWithABlockingFactorIsRefusedOnEitherSide() {
        ExperimentalFactor subject = categorical( "subject" ), treatment = categorical( "treatment" );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setBlockingFactors( Collections.singletonList( subject ) );
        assertThatThrownBy( () -> config.addInteractionToInclude( Arrays.asList( subject, treatment ) ) )
                .hasMessageContaining( "cannot also be in an interaction" );
        assertThatThrownBy( () -> config.addInteractionToInclude( Arrays.asList( treatment, subject ) ) )
                .hasMessageContaining( "cannot also be in an interaction" );
        assertThat( config.getInteractionsToInclude() ).isEmpty();
    }

    /**
     * An interaction between two non-blocking factors is unaffected by the blocking restriction.
     */
    @Test
    public void interactionBetweenOtherFactorsStillAllowedWithABlockingFactorSet() {
        ExperimentalFactor subject = categorical( "subject" ), treatment = categorical( "treatment" ), tissue = categorical( "tissue" );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setBlockingFactors( Collections.singletonList( subject ) );
        config.addInteractionToInclude( Arrays.asList( treatment, tissue ) );
        assertThat( config.getInteractionsToInclude() ).hasSize( 1 );
    }
}
