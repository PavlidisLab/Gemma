package ubic.gemma.core.util.math;

import cern.colt.list.DoubleArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link Distance}: alternative distance + similarity helpers used by
 * the coexpression and sample-clustering paths. Covers standardized Pearson,
 * Euclidean, Manhattan, and Spearman variants. Pins the NaN-aware semantics
 * (skip vs throw vs sentinel return value) — those are easy to regress on
 * refactor.
 *
 * @author claude
 */
public class DistanceTest {

    private static final double EPS = 1e-9;

    // ----------------------------------------------------------------------------------
    // correlationOfStandardized (double[] form): assumes pre-standardized inputs
    // ----------------------------------------------------------------------------------

    @Test
    public void correlationOfStandardized_array_identicalVectors_isOne() {
        // For standardized vectors of size n with sum of squares (n-1), x . x / (n-1) = 1.
        // Build a length-3 vector with mean 0, variance 1.
        double[] a = standardized( new double[] { 1, 2, 3 } );
        assertThat( Distance.correlationOfStandardized( a, a ) ).isCloseTo( 1.0, within( EPS ) );
    }

    @Test
    public void correlationOfStandardized_array_oppositeVectors_isNegativeOne() {
        double[] a = standardized( new double[] { 1, 2, 3 } );
        double[] negA = new double[a.length];
        for ( int i = 0; i < a.length; i++ ) negA[i] = -a[i];
        assertThat( Distance.correlationOfStandardized( a, negA ) ).isCloseTo( -1.0, within( EPS ) );
    }

    // ----------------------------------------------------------------------------------
    // correlationOfStandardized (DoubleArrayList form): handles missing values
    // ----------------------------------------------------------------------------------

    @Test
    public void correlationOfStandardized_list_mismatchedSizes_throws() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 1, 2 } );
        assertThatThrownBy( () -> Distance.correlationOfStandardized( x, y ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Array lengths must be the same" );
    }

    @Test
    public void correlationOfStandardized_list_allNaN_returnsSentinel() {
        DoubleArrayList x = new DoubleArrayList( new double[] { Double.NaN, Double.NaN } );
        DoubleArrayList y = new DoubleArrayList( new double[] { Double.NaN, Double.NaN } );
        // Documented sentinel -2.0 when all values are NaN.
        assertThat( Distance.correlationOfStandardized( x, y ) ).isEqualTo( -2.0 );
    }

    @Test
    public void correlationOfStandardized_list_skipsNaNs() {
        // The implementation skips NaN positions; equal standardized vectors at the
        // non-NaN positions should still yield ~1.
        double[] a = standardized( new double[] { 1, 2, 3 } );
        DoubleArrayList x = new DoubleArrayList( new double[] { a[0], Double.NaN, a[1], a[2] } );
        DoubleArrayList y = new DoubleArrayList( new double[] { a[0], 99.0, a[1], a[2] } ); // 99 is paired with NaN -> skipped
        // 3 used pairs -> sxy = 1*1+... ; same as the standardized self-correlation case.
        assertThat( Distance.correlationOfStandardized( x, y ) ).isCloseTo( 1.0, within( EPS ) );
    }

    // ----------------------------------------------------------------------------------
    // Euclidean distance
    // ----------------------------------------------------------------------------------

    @Test
    public void euclDistance_identicalVectors_isZero() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 1, 2, 3 } );
        assertThat( Distance.euclDistance( x, y ) ).isEqualTo( 0.0 );
    }

    @Test
    public void euclDistance_simpleCase_pythagorean() {
        // (3,4) from (0,0) -> 5.
        DoubleArrayList x = new DoubleArrayList( new double[] { 3, 4 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 0, 0 } );
        assertThat( Distance.euclDistance( x, y ) ).isCloseTo( 5.0, within( EPS ) );
    }

    @Test
    public void euclDistance_skipsNaNs() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 3, Double.NaN, 4 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 0, 99, 0 } );
        // NaN pair skipped; effectively (3,4) vs (0,0) -> 5
        assertThat( Distance.euclDistance( x, y ) ).isCloseTo( 5.0, within( EPS ) );
    }

    @Test
    public void euclDistance_mismatchedSizes_throws() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 1, 2, 3 } );
        assertThatThrownBy( () -> Distance.euclDistance( x, y ) )
                .isInstanceOf( ArithmeticException.class );
    }

    // ----------------------------------------------------------------------------------
    // Manhattan distance
    // ----------------------------------------------------------------------------------

    @Test
    public void manhattanDistance_identicalVectors_isZero() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3 } );
        assertThat( Distance.manhattanDistance( x, x ) ).isEqualTo( 0.0 );
    }

    @Test
    public void manhattanDistance_simpleCase() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 4, 6, 9 } );
        // |1-4| + |2-6| + |3-9| = 3 + 4 + 6 = 13
        assertThat( Distance.manhattanDistance( x, y ) ).isCloseTo( 13.0, within( EPS ) );
    }

    @Test
    public void manhattanDistance_skipsNaNs() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, Double.NaN, 3 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 4, 0, 9 } );
        assertThat( Distance.manhattanDistance( x, y ) ).isCloseTo( 9.0, within( EPS ) );
    }

    @Test
    public void manhattanDistance_mismatchedSizes_throws() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 1, 2, 3 } );
        assertThatThrownBy( () -> Distance.manhattanDistance( x, y ) )
                .isInstanceOf( ArithmeticException.class );
    }

    // ----------------------------------------------------------------------------------
    // Spearman rank correlation
    // ----------------------------------------------------------------------------------

    @Test
    public void spearmanRankCorrelation_pairs_perfectAgreement_isOne() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3, 4, 5 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 10, 20, 30, 40, 50 } );
        assertThat( Distance.spearmanRankCorrelation( x, y ) ).isCloseTo( 1.0, within( EPS ) );
    }

    @Test
    public void spearmanRankCorrelation_pairs_perfectDisagreement_isNegativeOne() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3, 4, 5 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 50, 40, 30, 20, 10 } );
        assertThat( Distance.spearmanRankCorrelation( x, y ) ).isCloseTo( -1.0, within( EPS ) );
    }

    @Test
    public void spearmanRankCorrelation_pairs_tooFewNonNaN_returnsNaN() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, Double.NaN, Double.NaN } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 10, 20, 30 } );
        assertThat( Distance.spearmanRankCorrelation( x, y ) ).isNaN();
    }

    @Test
    public void spearmanRankCorrelation_pairs_mismatchedSizes_throws() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2 } );
        DoubleArrayList y = new DoubleArrayList( new double[] { 1, 2, 3 } );
        assertThatThrownBy( () -> Distance.spearmanRankCorrelation( x, y ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void spearmanRankCorrelation_singleArg_ascendingIsOne() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 1, 2, 3, 4, 5 } );
        assertThat( Distance.spearmanRankCorrelation( x ) ).isCloseTo( 1.0, within( EPS ) );
    }

    @Test
    public void spearmanRankCorrelation_singleArg_descendingIsNegativeOne() {
        DoubleArrayList x = new DoubleArrayList( new double[] { 5, 4, 3, 2, 1 } );
        assertThat( Distance.spearmanRankCorrelation( x ) ).isCloseTo( -1.0, within( EPS ) );
    }

    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    /**
     * Center + standardize a small vector so its mean is 0 and sample variance is 1
     * (i.e. sum of squares is n-1). The Pearson-of-standardized contract expects this.
     */
    private static double[] standardized( double[] v ) {
        double mean = 0;
        for ( double d : v ) mean += d;
        mean /= v.length;
        double var = 0;
        for ( double d : v ) var += ( d - mean ) * ( d - mean );
        var /= ( v.length - 1 );
        double sd = Math.sqrt( var );
        double[] out = new double[v.length];
        for ( int i = 0; i < v.length; i++ ) out[i] = ( v[i] - mean ) / sd;
        return out;
    }
}
