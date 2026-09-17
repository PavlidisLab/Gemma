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
