package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * What a factor's columns look like under each {@link ContrastCoding}, and what its coefficients therefore mean.
 * <p>
 * The design matrix is the whole of the difference: both codings give a k-level factor k-1 columns, so the fit,
 * the rank and everything downstream keep their shape, and only the numbers in those columns change. Most of
 * these read the columns directly, because that is where the change is; the last two run a fit, because what the
 * columns are FOR is what the coefficients then mean, and only a fit can show that.
 */
public class ContrastCodingTest {

    /**
     * Treatment coding: the dropped level is all zeros, so each coefficient is a difference from it.
     */
    @Test
    public void treatmentCodingLeavesTheDroppedLevelAtZero() {
        DesignMatrix dm = new DesignMatrix( tissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );

        DoubleMatrix<String, String> m = dm.getMatrix();
        // intercept + (liver, kidney); cortex has no column of its own
        assertThat( m.columns() ).isEqualTo( 3 );
        assertThat( column( m, 1 ) ).containsExactly( 0.0, 0.0, 1.0, 1.0, 0.0, 0.0 );
        assertThat( column( m, 2 ) ).containsExactly( 0.0, 0.0, 0.0, 0.0, 1.0, 1.0 );
    }

    /**
     * 🛑 Sum-to-zero coding: the dropped level takes -1 in EVERY column of its factor. That is the whole
     * mechanism — it is what makes the intercept the mean of the level means rather than the dropped level's
     * mean, and each coefficient a deviation from that rather than a difference from a nominated control.
     * <p>
     * The column count is unchanged, which is the other half of the point: nothing downstream of the design
     * matrix has to know which coding was used in order to keep working.
     */
    @Test
    public void sumToZeroCodingPutsMinusOneInEveryColumnForTheDerivedLevel() {
        DesignMatrix dm = new DesignMatrix( tissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );
        dm.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        DoubleMatrix<String, String> m = dm.getMatrix();
        assertThat( m.columns() ).as( "same rank as treatment coding" ).isEqualTo( 3 );
        assertThat( column( m, 1 ) ).containsExactly( -1.0, -1.0, 1.0, 1.0, 0.0, 0.0 );
        assertThat( column( m, 2 ) ).containsExactly( -1.0, -1.0, 0.0, 0.0, 1.0, 1.0 );
    }

    /**
     * The coding is per factor, not per matrix — a genotype factor with a real wild-type control keeps treatment
     * coding while a tissue factor in the same design is deviation-coded. That combination is the reason the
     * setting is not one flag on the analysis.
     */
    @Test
    public void codingIsChosenPerFactorNotForTheWholeDesign() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 6, 2 );
        design.setColumnNames( java.util.Arrays.asList( "tissue", "genotype" ) );
        design.setRowNames( java.util.Arrays.asList( "s0", "s1", "s2", "s3", "s4", "s5" ) );
        String[] tissue = { "cortex", "cortex", "liver", "liver", "kidney", "kidney" };
        String[] genotype = { "wt", "ko", "wt", "ko", "wt", "ko" };
        for ( int i = 0; i < 6; i++ ) {
            design.set( i, 0, tissue[i] );
            design.set( i, 1, genotype[i] );
        }

        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setBaseline( "tissue", "cortex" );
        dm.setBaseline( "genotype", "wt" );
        dm.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        assertThat( dm.getContrastCoding( "tissue" ) ).isEqualTo( ContrastCoding.SUM_TO_ZERO );
        assertThat( dm.getContrastCoding( "genotype" ) ).isEqualTo( ContrastCoding.TREATMENT );

        DoubleMatrix<String, String> m = dm.getMatrix();
        // intercept, tissue x2 (sum-coded), genotype x1 (treatment-coded)
        assertThat( m.columns() ).isEqualTo( 4 );
        assertThat( column( m, 1 ) ).containsExactly( -1.0, -1.0, 1.0, 1.0, 0.0, 0.0 );
        assertThat( column( m, 3 ) )
                .as( "genotype keeps treatment coding: wt stays at zero rather than taking -1" )
                .containsExactly( 0.0, 1.0, 0.0, 1.0, 0.0, 1.0 );
    }

    /**
     * Setting the baseline after the coding has to give the same design as setting it before — both rebuild, and
     * a caller has no reason to know the order matters. If it did, the level taking -1 would depend on the order
     * two unrelated setters were called in, which is the kind of thing that is only ever found in a result.
     */
    @Test
    public void baselineAndCodingCommute() {
        DesignMatrix first = new DesignMatrix( tissueDesign(), true );
        first.setBaseline( "tissue", "liver" );
        first.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        DesignMatrix second = new DesignMatrix( tissueDesign(), true );
        second.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );
        second.setBaseline( "tissue", "liver" );

        DoubleMatrix<String, String> a = first.getMatrix();
        DoubleMatrix<String, String> b = second.getMatrix();
        assertThat( b.columns() ).isEqualTo( a.columns() );
        for ( int i = 0; i < a.rows(); i++ ) {
            for ( int j = 0; j < a.columns(); j++ ) {
                assertThat( b.get( i, j ) ).as( "cell (%d,%d)", i, j ).isEqualTo( a.get( i, j ), offset( 1e-12 ) );
            }
        }
        // liver is the derived level in both, so it is the one carrying -1
        assertThat( column( a, 1 ) ).contains( -1.0 );
    }

    /**
     * 🛑 The end of the contract, through an actual fit: under SUM_TO_ZERO the intercept is the mean of the LEVEL
     * means and each coefficient is that level's deviation from it.
     * <p>
     * The groups are deliberately unequal in size — 2, 4 and 1 sample — because on a balanced design the mean
     * of the level means and the mean of the samples coincide, and a test built on one could not tell which
     * reference the coefficients were against. Here they are 21.333 and 20.571.
     * <p>
     * Level means 11, 23 and 30; their mean 64/3. Cortex is the derived level, so its deviation has no
     * coefficient of its own and is minus the sum of the other two — which is the number the caller has to
     * reconstruct, and the reason the analyzer synthesizes a contrast for it rather than leaving it absent.
     */
    @Test
    public void sumToZeroCoefficientsAreDeviationsFromTheMeanOfTheLevelMeans() {
        DesignMatrix dm = new DesignMatrix( unbalancedTissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );
        dm.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        LeastSquaresFit fit = new LeastSquaresFit( dm, oneRow( 10, 12, 20, 22, 24, 26, 30 ) );
        DoubleMatrix2D coef = fit.getCoefficients();

        double mean = 64.0 / 3.0;
        assertThat( coef.get( 0, 0 ) ).as( "intercept is the mean of the level means, not of the samples" )
                .isCloseTo( mean, offset( 1e-9 ) )
                .isNotCloseTo( 144.0 / 7.0, offset( 1e-3 ) );
        assertThat( coef.get( 1, 0 ) ).as( "liver deviation" ).isCloseTo( 23.0 - mean, offset( 1e-9 ) );
        assertThat( coef.get( 2, 0 ) ).as( "kidney deviation" ).isCloseTo( 30.0 - mean, offset( 1e-9 ) );

        double cortex = -( coef.get( 1, 0 ) + coef.get( 2, 0 ) );
        assertThat( cortex ).as( "the derived level's deviation" ).isCloseTo( 11.0 - mean, offset( 1e-9 ) );
        assertThat( coef.get( 1, 0 ) + coef.get( 2, 0 ) + cortex )
                .as( "deviations sum to zero by construction" ).isCloseTo( 0.0, offset( 1e-9 ) );
    }

    /**
     * The same data under TREATMENT coding, so the difference is visible rather than asserted. Here the intercept
     * is cortex's own mean and every coefficient is a difference from cortex — which is the right answer when
     * cortex is a control and the wrong question when it is just one tissue of three.
     */
    @Test
    public void treatmentCoefficientsAreDifferencesFromTheBaselineLevel() {
        DesignMatrix dm = new DesignMatrix( unbalancedTissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );

        LeastSquaresFit fit = new LeastSquaresFit( dm, oneRow( 10, 12, 20, 22, 24, 26, 30 ) );
        DoubleMatrix2D coef = fit.getCoefficients();

        assertThat( coef.get( 0, 0 ) ).as( "intercept is cortex's mean" ).isCloseTo( 11.0, offset( 1e-9 ) );
        assertThat( coef.get( 1, 0 ) ).as( "liver minus cortex" ).isCloseTo( 12.0, offset( 1e-9 ) );
        assertThat( coef.get( 2, 0 ) ).as( "kidney minus cortex" ).isCloseTo( 19.0, offset( 1e-9 ) );
    }

    /**
     * The derived level's columns are found, and they are exactly its own factor's.
     * <p>
     * This is the guard against the silent half of {@link DesignMatrix#getDerivedLevelColumns()}: it declines to
     * report a factor whose columns do not come out at one per non-derived level, so a naming change would turn
     * the whole feature off rather than compute it wrongly. If that ever happens this test is what says so.
     */
    @Test
    public void theDerivedLevelsColumnsAreFoundAndAreOnlyItsOwnFactors() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 6, 2 );
        design.setColumnNames( java.util.Arrays.asList( "tissue", "genotype" ) );
        design.setRowNames( java.util.Arrays.asList( "s0", "s1", "s2", "s3", "s4", "s5" ) );
        String[] tissue = { "cortex", "cortex", "liver", "liver", "kidney", "kidney" };
        String[] genotype = { "wt", "ko", "wt", "ko", "wt", "ko" };
        for ( int i = 0; i < 6; i++ ) {
            design.set( i, 0, tissue[i] );
            design.set( i, 1, genotype[i] );
        }
        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setBaseline( "tissue", "cortex" );
        dm.setBaseline( "genotype", "wt" );
        dm.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        java.util.Map<String, java.util.List<Integer>> derived = dm.getDerivedLevelColumns();

        assertThat( derived ).as( "only the sum-coded factor needs a derived contrast" )
                .containsOnlyKeys( "tissuecortex" );
        java.util.List<Integer> cols = derived.get( "tissuecortex" );
        assertThat( cols ).as( "two non-derived tissue levels, and the genotype column is not one of them" )
                .hasSize( 2 );
        java.util.List<String> names = dm.getMatrix().getColNames();
        for ( int c : cols ) {
            assertThat( names.get( c ) ).startsWith( "tissue" ).isNotEqualTo( "tissuecortex" );
        }
    }

    /**
     * 🛑 The synthesized contrast has to be the SAME contrast, standard error included.
     * <p>
     * Which level is derived is an arbitrary choice, so fitting the same data with cortex derived and then with
     * liver derived must give cortex the same deviation, standard error, t and p either way — estimated in one
     * fit and reconstructed in the other. This is the check that the reconstruction is a real contrast and not
     * a number that merely has the right sign.
     * <p>
     * It is the standard error that this actually tests. The coefficient is minus the sum of the others by
     * construction and would come out right under any arithmetic; the error requires the whole covariance block
     * {@code 1' XtXi 1}. Summing the individual variances instead — the obvious wrong version — ignores the
     * covariance between the estimates, which is not zero and has no fixed sign: on this design it is negative,
     * so the wrong version comes out too LARGE (0.898 against 0.601). It would be too small on another design,
     * which is why the assertion below pins "different" rather than a direction.
     */
    @Test
    public void theDerivedLevelsContrastMatchesEstimatingItDirectly() {
        DesignMatrix cortexDerived = new DesignMatrix( unbalancedTissueDesign(), true );
        cortexDerived.setBaseline( "tissue", "cortex" );
        cortexDerived.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        DesignMatrix liverDerived = new DesignMatrix( unbalancedTissueDesign(), true );
        liverDerived.setBaseline( "tissue", "liver" );
        liverDerived.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        DoubleMatrix<String, String> synthesized = contrastRow(
                new LeastSquaresFit( cortexDerived, oneRow( 10, 12, 20, 22, 24, 26, 30 ) ) );
        DoubleMatrix<String, String> estimated = contrastRow(
                new LeastSquaresFit( liverDerived, oneRow( 10, 12, 20, 22, 24, 26, 30 ) ) );

        int syn = synthesized.getRowIndexByName( "tissuecortex" );
        int est = estimated.getRowIndexByName( "tissuecortex" );

        for ( int c = 0; c < 4; c++ ) {
            assertThat( synthesized.get( syn, c ) )
                    .as( "%s for cortex: synthesized vs estimated", estimated.getColName( c ) )
                    .isCloseTo( estimated.get( est, c ), offset( 1e-9 ) );
        }
        // and it is a real number, not a NaN that trivially matched nothing
        assertThat( synthesized.get( syn, 0 ) ).isCloseTo( 11.0 - 64.0 / 3.0, offset( 1e-9 ) );
        assertThat( synthesized.get( syn, 1 ) ).isGreaterThan( 0.0 );

        /*
         * The wrong version, stated so it cannot be reintroduced quietly: adding the two coefficients'
         * variances ignores the covariance between them, and the covariance is not zero.
         */
        double vLiver = Math.pow( synthesized.get( synthesized.getRowIndexByName( "tissueliver" ), 1 ), 2 );
        double vKidney = Math.pow( synthesized.get( synthesized.getRowIndexByName( "tissuekidney" ), 1 ), 2 );
        assertThat( Math.sqrt( vLiver + vKidney ) )
                .as( "the diagonal alone is not the variance of the sum" )
                .isNotCloseTo( synthesized.get( syn, 1 ), offset( 1e-6 ) );
    }

    /** Every level of a sum-coded factor gets a row, including the one with no column. */
    @Test
    public void everyLevelGetsAContrastUnderSumToZero() {
        DesignMatrix dm = new DesignMatrix( unbalancedTissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );
        dm.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        DoubleMatrix<String, String> rows = contrastRow(
                new LeastSquaresFit( dm, oneRow( 10, 12, 20, 22, 24, 26, 30 ) ) );

        assertThat( rows.getRowNames() ).contains( "tissuecortex", "tissueliver", "tissuekidney" );
    }

    /**
     * Treatment coding is untouched: the baseline still has no row, because there is no contrast for it — it is
     * what everything else is measured against, not a thing measured against something.
     */
    @Test
    public void treatmentCodingStillHasNoRowForTheBaseline() {
        DesignMatrix dm = new DesignMatrix( unbalancedTissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );

        DoubleMatrix<String, String> rows = contrastRow(
                new LeastSquaresFit( dm, oneRow( 10, 12, 20, 22, 24, 26, 30 ) ) );

        assertThat( rows.getRowNames() ).contains( "tissueliver", "tissuekidney" )
                .doesNotContain( "tissuecortex" );
    }

    private static DoubleMatrix<String, String> contrastRow( LeastSquaresFit fit ) {
        return fit.summarize().get( 0 ).getContrastCoefficients();
    }

    /**
     * Residual standard error of the fixture fit, from R. Gemma's summary table reports the UNSCALED standard
     * error, so this is what turns it into the one R prints.
     */
    private static final double R_SIGMA = 2.34520787991171;

    /**
     * How close to R is close enough. Loose enough not to be a hostage to the BLAS a machine happens to have,
     * tight enough to be worth asserting: the smallest p here is 3.3e-05, and the covariance-block error this
     * suite was written to catch was off by 0.16 on a value of 0.60.
     */
    private static final double R_TOLERANCE = 1e-6;

    /**
     * 🛑 Benchmarked against R. The fits that produced every number below are in
     * {@code src/test/resources/data/stat-tests/contrast-coding.R}, which regenerates them and carries the
     * output as comments; these tests depend on them not changing.
     * <p>
     * The level order there is not the obvious one and the script says why: R's {@code contr.sum} drops the
     * LAST level, Gemma drops the FIRST — the one {@code setBaseline} names — so R's factor puts Gemma's
     * baseline last to compare like with like.
     * <p>
     * ⚠️ Gemma's "Std. Error" column holds the UNSCALED value ({@code sqrt(diag(cov.unscaled))}), not the
     * standard error R prints. They differ by sigma, and the estimate, t and p are directly comparable while
     * that one is not. Both forms are asserted here so the difference is recorded rather than rediscovered.
     */
    @Test
    public void sumToZeroMatchesRContrSum() {
        DesignMatrix dm = new DesignMatrix( unbalancedTissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );
        dm.setContrastCoding( "tissue", ContrastCoding.SUM_TO_ZERO );

        DoubleMatrix<String, String> got = contrastRow(
                new LeastSquaresFit( dm, oneRow( 10, 12, 20, 22, 24, 26, 30 ) ) );

        // R: (Intercept) 21.33333333333334  1.03413947049924  20.62906787904974  3.26181828716962e-05
        assertRRow( got, "(Intercept)", 21.33333333333334, 0.440958551844098, 1.03413947049924,
                20.62906787904974, 3.26181828716962e-05 );
        // R: tissue1 (liver) 1.66666666666667  1.23603308118261  1.34839972492648  2.48820913808664e-01
        assertRRow( got, "tissueliver", 1.66666666666667, 0.527046276694730, 1.23603308118261,
                1.34839972492648, 2.48820913808664e-01 );
        // R: tissue2 (kidney) 8.66666666666666  1.70375402502174  5.08680627566299  7.04714642242505e-03
        assertRRow( got, "tissuekidney", 8.66666666666666, 0.726483157256779, 1.70375402502174,
                5.08680627566299, 7.04714642242505e-03 );
        /*
         * The derived level, which R has no row for under this parameterization. The reference values come
         * from refitting in R with liver derived instead, where cortex IS estimated:
         *   tissue2 <- factor(..., levels = c("cortex","kidney","liver")); contrasts(tissue2) <- contr.sum(3)
         *   summary(lm(y ~ tissue2))  ->  -10.33333333333333  1.40929454377398  -7.33227371026462  1.841510578397e-03
         * R reaches the same numbers the other way too, from -sum(b) and sum(cov.unscaled[2:3,2:3]).
         */
        assertRRow( got, "tissuecortex", -10.33333333333333, 0.600925212577331, 1.40929454377398,
                -7.33227371026462, 1.841510578397e-03 );
    }

    /**
     * Treatment coding against R, so the sum-to-zero work is shown not to have disturbed the path everything
     * else uses. Last block of {@code contrast-coding.R}; R's default, no {@code contrasts()} call needed.
     */
    @Test
    public void treatmentCodingMatchesRContrTreatment() {
        DesignMatrix dm = new DesignMatrix( unbalancedTissueDesign(), true );
        dm.setBaseline( "tissue", "cortex" );

        DoubleMatrix<String, String> got = contrastRow(
                new LeastSquaresFit( dm, oneRow( 10, 12, 20, 22, 24, 26, 30 ) ) );

        assertRRow( got, "(Intercept)", 11.0, 1.65831239517770 / R_SIGMA, 1.65831239517770,
                6.63324958071080, 0.00268009608714780 );
        assertRRow( got, "tissueliver", 12.0, 2.03100960115899 / R_SIGMA, 2.03100960115899,
                5.90839156700797, 0.00410747546615996 );
        assertRRow( got, "tissuekidney", 19.0, 2.87228132326901 / R_SIGMA, 2.87228132326901,
                6.61495092631652, 0.00270778321164432 );
    }

    /**
     * @param unscaledSe what Gemma stores in "Std. Error"
     * @param rSe        what R prints as Std. Error; {@code unscaledSe * sigma}
     */
    private static void assertRRow( DoubleMatrix<String, String> got, String row, double estimate,
            double unscaledSe, double rSe, double t, double p ) {
        int i = got.getRowIndexByName( row );
        assertThat( got.get( i, 0 ) ).as( "%s estimate", row ).isCloseTo( estimate, offset( R_TOLERANCE ) );
        assertThat( got.get( i, 1 ) ).as( "%s unscaled std. error", row )
                .isCloseTo( unscaledSe, offset( R_TOLERANCE ) );
        assertThat( got.get( i, 1 ) * R_SIGMA ).as( "%s std. error as R prints it", row )
                .isCloseTo( rSe, offset( R_TOLERANCE ) );
        assertThat( got.get( i, 2 ) ).as( "%s t value", row ).isCloseTo( t, offset( R_TOLERANCE ) );
        assertThat( got.get( i, 3 ) ).as( "%s p value", row ).isCloseTo( p, offset( R_TOLERANCE ) );
    }

    /** Naming a factor that is not in the design is a caller error, not a silent no-op. */
    @Test
    public void anUnknownFactorIsRejected() {
        DesignMatrix dm = new DesignMatrix( tissueDesign(), true );
        assertThatThrownBy( () -> dm.setContrastCoding( "nosuchfactor", ContrastCoding.SUM_TO_ZERO ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "tissue" );
    }

    /** Seven samples over three tissues, 2 / 4 / 1 — unequal on purpose. */
    private static ObjectMatrix<String, String, Object> unbalancedTissueDesign() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 7, 1 );
        design.setColumnNames( java.util.Collections.singletonList( "tissue" ) );
        design.setRowNames( java.util.Arrays.asList( "s0", "s1", "s2", "s3", "s4", "s5", "s6" ) );
        String[] tissue = { "cortex", "cortex", "liver", "liver", "liver", "liver", "kidney" };
        for ( int i = 0; i < 7; i++ ) {
            design.set( i, 0, tissue[i] );
        }
        return design;
    }

    /** One "probe" of data, named so it lines up with the design's rows. */
    private static DoubleMatrix<String, String> oneRow( double... values ) {
        DoubleMatrix<String, String> m = new ubic.gemma.core.util.matrix.DenseDoubleMatrix<>( 1, values.length );
        m.setRowName( "probe", 0 );
        for ( int j = 0; j < values.length; j++ ) {
            m.setColumnName( "s" + j, j );
            m.set( 0, j, values[j] );
        }
        return m;
    }

    /** Six samples over three tissues, two each. */
    private static ObjectMatrix<String, String, Object> tissueDesign() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 6, 1 );
        design.setColumnNames( java.util.Collections.singletonList( "tissue" ) );
        design.setRowNames( java.util.Arrays.asList( "s0", "s1", "s2", "s3", "s4", "s5" ) );
        String[] tissue = { "cortex", "cortex", "liver", "liver", "kidney", "kidney" };
        for ( int i = 0; i < 6; i++ ) {
            design.set( i, 0, tissue[i] );
        }
        return design;
    }

    private static Double[] column( DoubleMatrix<String, String> m, int j ) {
        Double[] out = new Double[m.rows()];
        for ( int i = 0; i < m.rows(); i++ ) {
            out[i] = m.get( i, j );
        }
        return out;
    }
}
