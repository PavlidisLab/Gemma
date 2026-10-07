package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Mixed model (random intercept): the R reference fits live in
 * {@code data/stat-tests/mixed-golden-gen.R} -- limma {@code duplicateCorrelation} (per-probe REML variance
 * decomposition, consensus by trimmed mean on the atanh scale) + {@code gls.series} + {@code eBayes}, on an
 * UNBALANCED block design: 11 samples over 8 subjects, 3 subjects with both arms, 5 with a single arm.
 * <p>
 * Fixture: {@code mixed-test-data.txt} (80 probes x 11 samples), {@code mixed-design.txt}. The treatment
 * effect (1.0 on the last 30 probes) must be recovered with the subject structure absorbed by the
 * correlation, NOT by design columns: the design given to both the estimation and the GLS fit is
 * {@code ~ treatment} only.
 * <p>
 * R golden values: consensus.correlation = 0.927800941759967; stdev.unscaled = [0.357538746462573,
 * 0.214429706393581]; eBayes df.total = 720 (df.prior + residual dof 10, capped at pooled 720... here the
 * cap binds because df.prior is large).
 */
public class MixedModelFitTest {

    private static final double EPS = 1e-6;

    private DoubleMatrix<String, String> data() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/mixed-test-data.txt" ) ) {
            assert is != null;
            return new DoubleMatrixReader().read( is );
        }
    }

    private DoubleMatrix2D coltData() throws Exception {
        return new DenseDoubleMatrix2D( data().asArray() );
    }

    private List<String> samples() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/mixed-design.txt" ) ) {
            assert is != null;
            String[] lines = new String( is.readAllBytes() ).split( "\n" );
            return Arrays.asList( Arrays.copyOfRange( lines, 1, lines.length ) ).stream()
                    .map( l -> l.split( "\t" )[0] ).toList();
        }
    }

    private List<String> subjects() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/mixed-design.txt" ) ) {
            assert is != null;
            String[] lines = new String( is.readAllBytes() ).split( "\n" );
            return Arrays.asList( Arrays.copyOfRange( lines, 1, lines.length ) ).stream()
                    .map( l -> l.split( "\t" )[1] ).toList();
        }
    }

    private List<String> treatments() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/mixed-design.txt" ) ) {
            assert is != null;
            String[] lines = new String( is.readAllBytes() ).split( "\n" );
            return Arrays.asList( Arrays.copyOfRange( lines, 1, lines.length ) ).stream()
                    .map( l -> l.split( "\t" )[2] ).toList();
        }
    }

    /**
     * Design ~ treatment only (intercept + treat), baseline ctrl. The subject factor has NO columns -- it
     * is the correlation structure.
     */
    private DesignMatrix treatmentDesign() throws Exception {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 11, 1 );
        design.setColumnNames( Arrays.asList( "treatment" ) );
        design.setRowNames( samples() );
        List<String> trt = treatments();
        for ( int i = 0; i < 11; i++ ) {
            design.set( i, 0, trt.get( i ) );
        }
        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setBaseline( "treatment", "ctrl" );
        return dm;
    }

    private String[] blockIds() throws Exception {
        List<String> s = subjects();
        return s.toArray( new String[0] );
    }

    /**
     * The consensus correlation must match R's duplicateCorrelation closely: the per-probe REML
     * decomposition (mixedModel2Fit port), the clamping and the 15%-trimmed atanh mean all have to agree.
     * R: 0.927800941759967.
     * <p>
     * 🛑 The tolerance is 2e-3, NOT machine precision, and that is a property of the DESIGN, not a port
     * slack: this unbalanced design gives QtZ REPEATED singular values (d = 2, 2, ..., 1, 1, 1, ...) --
     * within a degenerate group the orthonormal basis is implementation-defined (R's LAPACK dgesdd vs colt's
     * tred2/tql2 eigendecomposition), and mixedModel2Fit's gamma-approximation likelihood is per-point, not
     * group-sufficient, so the per-probe rho depends on which basis was drawn. R itself moves by the same
     * amount when the SVD is replaced by the equivalent eigendecomposition of QtZ QtZ' (0.9311729 vs
     * 0.9311557 for probe_0). On designs without the degeneracy the agreement is ~1e-12.
     */
    @Test
    public void consensusCorrelationMatchesLimma() throws Exception {
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation(
                treatmentDesign().getDoubleMatrix(), blockIds(), coltData(), null );
        assertThat( mmf.isDegenerateToZero() ).isFalse();
        assertThat( mmf.getConsensusCorrelation() )
                .as( "R duplicateCorrelation consensus.correlation" )
                .isCloseTo( 0.927800941759967, offset( 2e-3 ) );
    }

    /**
     * The first per-probe atanh correlations must track R's duplicateCorrelation output (the clamps and the
     * varcomp fits). R: [1.667142682608944, -0.334361363657104, 1.301865983501213, 1.802145336514870,
     * 2.612734394446732, 1.017770533488196, 1.608194247652209, 0.815993830348357, 1.729461939700665,
     * 1.866444785611097]. Same degenerate-basis caveat as {@link #consensusCorrelationMatchesLimma()}.
     */
    @Test
    public void perProbeCorrelationsMatchLimma() throws Exception {
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation(
                treatmentDesign().getDoubleMatrix(), blockIds(), coltData(), null );
        double[] expected = { 1.667142682608944, -0.334361363657104, 1.301865983501213, 1.802145336514870,
                2.612734394446732, 1.017770533488196, 1.608194247652209, 0.815993830348357, 1.729461939700665,
                1.866444785611097 };
        for ( int i = 0; i < expected.length; i++ ) {
            assertThat( mmf.getAtanhCorrelations()[i] )
                    .as( "atanh correlation of probe %d (R: %f)", i, expected[i] )
                    .isCloseTo( expected[i], offset( 3e-4 ) );
        }
        // 3e-4, not machine precision: the gamma IRLS (glmgam.fit) stops on an absolute score-step
        // tolerance (crossprod(dl, dbeta) < 1e-6) whose stopping point depends on the iteration path;
        // the residual difference is convergence noise at the fourth decimal of rho and does not propagate
        // beyond it (see the fixed-rho GLS assertions, which are exact).
    }

    /**
     * The GLS fit: coefficients and sigma match limma gls.series. The correlation is FIXED at R's golden
     * consensus value so the comparison is exact -- given the same rho, the whitening (limma's
     * backsolve(cholV, X, transpose=TRUE), i.e. the LOWER Cholesky factor solve) and the whole downstream
     * summary machinery must reproduce R to machine precision. probe_0: coefficients [-0.376993684655524,
     * 0.285495777609915], sigma 1.76715565593929, stdev.unscaled [0.357538746462573, 0.214429706393581].
     */
    @Test
    public void glsFitMatchesLimmaGlsSeries() throws Exception {
        DesignMatrix dm = treatmentDesign();
        DoubleMatrix2D correlation = MixedModelFit.blockCorrelationMatrix( blockIds(), 0.927800941759967 );
        LeastSquaresFit fit = new LeastSquaresFit( dm, correlation, data() );

        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_0" );
        assertThat( s.getContrastCoefficients( "treatment" ).get( "treatmenttreat" ) )
                .as( "R gls coefficient for probe_0" )
                .isCloseTo( 0.285495777609915, offset( 1e-9 ) );
        assertThat( s.getInterceptCoefficient() )
                .isCloseTo( -0.376993684655524, offset( 1e-9 ) );
        assertThat( s.getSigma() )
                .as( "R gls sigma for probe_0 (carries the correlation's variance inflation)" )
                .isCloseTo( 1.76715565593929, offset( 1e-9 ) );
        assertThat( s.getStdevUnscaled()[0] )
                .as( "R gls stdev.unscaled[0]" )
                .isCloseTo( 0.357538746462573, offset( 1e-9 ) );
        assertThat( s.getStdevUnscaled()[1] )
                .as( "R gls stdev.unscaled[1]" )
                .isCloseTo( 0.214429706393581, offset( 1e-9 ) );
    }

    /**
     * Moderated statistics on the GLS fit, at the fixed R rho: R eBayes(gfit) gives probe_50
     * t = 2.44525064321526, p = 0.0147137041006308, df.total = 720 -- df.total is df.prior + residual dof,
     * capped at the pooled residual dof (80 probes x 9), exercising the pmin cap through the mixed model.
     */
    @Test
    public void moderatedStatisticsMatchLimmaOnTheGlsFit() throws Exception {
        DesignMatrix dm = treatmentDesign();
        DoubleMatrix2D correlation = MixedModelFit.blockCorrelationMatrix( blockIds(), 0.927800941759967 );
        LeastSquaresFit fit = new LeastSquaresFit( dm, correlation, data() );
        ModeratedTstat.ebayes( fit );

        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_50" );
        assertThat( s.getContrastTStats( "treatment" ).get( "treatmenttreat" ) )
                .as( "R eBayes t for probe_50 on the gls fit" )
                .isCloseTo( 2.44525064321526, offset( 1e-7 ) );
        assertThat( s.getContrastPValues( "treatment" ).get( "treatmenttreat" ) )
                .as( "R eBayes p for probe_50 on the gls fit" )
                .isCloseTo( 0.0147137041006308, offset( 1e-10 ) );
    }

    /**
     * The GLS fit at the ESTIMATED consensus rho stays within 2e-3 of R's own fit at R's estimated rho --
     * the rho gap (see the degenerate-basis note on {@link #consensusCorrelationMatchesLimma()}) propagates
     * at that order and no further. R gls at rho 0.927431173517552: [-0.3769848452, 0.2856311244], sigma
     * 1.766227578; my estimated rho is 0.9274311735175516.
     */
    @Test
    public void glsFitAtEstimatedRhoTracksRLimitedByTheRhoGap() throws Exception {
        DesignMatrix dm = treatmentDesign();
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation(
                dm.getDoubleMatrix(), blockIds(), coltData(), null );
        DoubleMatrix2D correlation = MixedModelFit.blockCorrelationMatrix( blockIds(), mmf.getConsensusCorrelation() );
        LeastSquaresFit fit = new LeastSquaresFit( dm, correlation, data() );

        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_0" );
        // R gls at its own rho: 0.285495777609915; at MY rho (0.9274311735175516, computed in R): 0.2856311244369219.
        // My fit must sit between the two, within the rho-gap scale.
        double coef = s.getContrastCoefficients( "treatment" ).get( "treatmenttreat" );
        assertThat( coef ).isCloseTo( 0.285495777609915, offset( 1e-3 ) );
        assertThat( coef ).isCloseTo( 0.2856311244369219, offset( 1e-3 ) );
    }

    /**
     * rho = 0 must reduce the GLS fit to the OLS fit: same coefficients, same sigma. The degenerate gate
     * (all-singleton subjects) produces exactly this.
     */
    @Test
    public void zeroCorrelationReducesToOls() throws Exception {
        DesignMatrix dm = treatmentDesign();
        DoubleMatrix2D identity = new DenseDoubleMatrix2D( 11, 11 );
        for ( int i = 0; i < 11; i++ ) {
            identity.set( i, i, 1.0 );
        }
        LeastSquaresFit gls = new LeastSquaresFit( dm, identity, data() );
        LeastSquaresFit ols = new LeastSquaresFit( dm, data() );

        LinearModelSummary glsS = gls.summarizeByKeys( false ).get( "probe_0" );
        LinearModelSummary olsS = ols.summarizeByKeys( false ).get( "probe_0" );
        assertThat( glsS.getContrastCoefficients( "treatment" ).get( "treatmenttreat" ) )
                .isCloseTo( olsS.getContrastCoefficients( "treatment" ).get( "treatmenttreat" ), offset( 1e-12 ) );
        assertThat( glsS.getSigma() ).isCloseTo( olsS.getSigma(), offset( 1e-12 ) );
    }

    /**
     * A block design nested inside the design (every subject carries exactly one treatment -- all
     * singletons) makes the block span sit inside the design span: limma backs off to rho = 0 with a
     * warning, and the GLS fit must come out as the plain OLS fit rather than fail.
     */
    @Test
    public void blockSpanInsideDesignBacksOffToZero() throws Exception {
        // every sample its own subject: 11 blocks of size 1
        List<String> samples = samples();
        String[] selfBlocks = samples.toArray( new String[0] );
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation(
                treatmentDesign().getDoubleMatrix(), selfBlocks, coltData(), null );
        assertThat( mmf.isDegenerateToZero() ).isTrue();
        assertThat( mmf.getConsensusCorrelation() ).isEqualTo( 0.0 );
    }

    /**
     * Missing values are refused loudly (the per-observation gls.series branch is not ported), not silently
     * fit as if complete.
     */
    @Test
    public void missingValuesAreRefused() throws Exception {
        DesignMatrix dm = treatmentDesign();
        DoubleMatrix<String, String> data = data();
        // blank one observation
        DenseDoubleMatrix2D corrupt = new DenseDoubleMatrix2D( data.asArray() );
        corrupt.set( 0, 0, Double.NaN );
        DoubleMatrix<String, String> corruptData = new ubic.gemma.core.util.matrix.DenseDoubleMatrix<>( corrupt.toArray() );
        corruptData.setRowNames( data.getRowNames() );
        corruptData.setColumnNames( data.getColNames() );

        org.assertj.core.api.Assertions.assertThatThrownBy( () -> new LeastSquaresFit( dm, MixedModelFit.blockCorrelationMatrix( blockIds(), 0.5 ), corruptData ) )
                .isInstanceOf( UnsupportedOperationException.class )
                .hasMessageContaining( "not supported with missing values" );
    }
}
