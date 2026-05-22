package ubic.gemma.core.util.math;

import cern.colt.list.DoubleArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link CorrelationStats} — the static stats facade used across
 * coexpression, batch-confound detection, and PCA-correlate analyses. Covers the
 * deterministic, lookup-free entry points: byte encoding/decoding, Fisher
 * transform + inverse, validity check, and edge cases of p-value computation.
 *
 * @author claude
 */
public class CorrelationStatsTest {

    private static final double EPS = 1e-9;

    // ----------------------------------------------------------------------------------
    // byteToCorrel / correlAsByte round-trip
    // ----------------------------------------------------------------------------------

    @Test
    public void correlAsByte_zero_isOneBelowHalfRange() {
        // ceil((0+1)*128) - 1 = 128 - 1 = 127
        assertThat( CorrelationStats.correlAsByte( 0.0 ) ).isEqualTo( 127 );
    }

    @Test
    public void correlAsByte_atOne_maps255() {
        // ceil((1+1)*128) - 1 = 256 - 1 = 255
        assertThat( CorrelationStats.correlAsByte( 1.0 ) ).isEqualTo( 255 );
    }

    @Test
    public void correlAsByte_atNegativeOne_maps0() {
        // -1.0 is special-cased to 0
        assertThat( CorrelationStats.correlAsByte( -1.0 ) ).isEqualTo( 0 );
    }

    @Test
    public void byteToCorrel_zero_returnsNegativeOne() {
        assertThat( CorrelationStats.byteToCorrel( 0 ) ).isEqualTo( -1.0 );
    }

    @Test
    public void byteToCorrel_128_returnsZero() {
        assertThat( CorrelationStats.byteToCorrel( 128 ) ).isEqualTo( 0.0 );
    }

    @Test
    public void byteToCorrel_255_returnsAlmostOne() {
        // 255/128 - 1 = 0.9921875
        assertThat( CorrelationStats.byteToCorrel( 255 ) ).isCloseTo( 0.9921875, within( EPS ) );
    }

    @Test
    public void correlAsByte_byteToCorrel_roundTrip_isLossyButBounded() {
        // round-trip is bounded by the 1/128 quantization step (about 0.0078)
        double[] values = { -0.95, -0.5, -0.1, 0.0, 0.1, 0.5, 0.95 };
        for ( double v : values ) {
            int b = CorrelationStats.correlAsByte( v );
            double recovered = CorrelationStats.byteToCorrel( b );
            assertThat( recovered ).as( "v=%f", v ).isCloseTo( v, within( 1.0 / 128.0 ) );
        }
    }

    // ----------------------------------------------------------------------------------
    // Fisher transform
    // ----------------------------------------------------------------------------------

    @Test
    public void fisherTransform_atZero_returnsZero() {
        assertThat( CorrelationStats.fisherTransform( 0.0 ) ).isEqualTo( 0.0 );
    }

    @Test
    public void fisherTransform_atOne_returnsPositiveInfinity() {
        assertThat( CorrelationStats.fisherTransform( 1.0 ) ).isEqualTo( Double.POSITIVE_INFINITY );
    }

    @Test
    public void fisherTransform_atNegativeOne_returnsPositiveInfinity() {
        // The implementation uses abs() so both +1 and -1 return POSITIVE_INFINITY.
        assertThat( CorrelationStats.fisherTransform( -1.0 ) ).isEqualTo( Double.POSITIVE_INFINITY );
    }

    @Test
    public void fisherTransform_atHalf_isClassicValue() {
        // atanh(0.5) = 0.5 * ln(1.5/0.5) = 0.5493061443340549
        assertThat( CorrelationStats.fisherTransform( 0.5 ) ).isCloseTo( 0.5493061443340549, within( EPS ) );
    }

    @Test
    public void fisherTransform_outOfRange_throws() {
        assertThatThrownBy( () -> CorrelationStats.fisherTransform( 1.5 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Invalid correlation" );
        assertThatThrownBy( () -> CorrelationStats.fisherTransform( -1.5 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void fisherTransform_list_appliesPerElement() {
        DoubleArrayList in = new DoubleArrayList( new double[] { 0.0, 0.5, -0.5 } );
        DoubleArrayList out = CorrelationStats.fisherTransform( in );
        assertThat( out.size() ).isEqualTo( 3 );
        assertThat( out.get( 0 ) ).isEqualTo( 0.0 );
        assertThat( out.get( 1 ) ).isCloseTo( 0.5493061443340549, within( EPS ) );
        assertThat( out.get( 2 ) ).isCloseTo( -0.5493061443340549, within( EPS ) );
    }

    @Test
    public void unFisherTransform_invertsFisherTransform() {
        for ( double r : new double[] { -0.95, -0.5, -0.1, 0.0, 0.1, 0.5, 0.95 } ) {
            double z = CorrelationStats.fisherTransform( r );
            double r2 = CorrelationStats.unFisherTransform( z );
            assertThat( r2 ).as( "r=%f", r ).isCloseTo( r, within( EPS ) );
        }
    }

    // ----------------------------------------------------------------------------------
    // isValidPearsonCorrelation
    // ----------------------------------------------------------------------------------

    @Test
    public void isValidPearsonCorrelation_acceptsInRangeValues() {
        assertThat( CorrelationStats.isValidPearsonCorrelation( -1.0 ) ).isTrue();
        assertThat( CorrelationStats.isValidPearsonCorrelation( 0.0 ) ).isTrue();
        assertThat( CorrelationStats.isValidPearsonCorrelation( 1.0 ) ).isTrue();
        assertThat( CorrelationStats.isValidPearsonCorrelation( 0.5 ) ).isTrue();
    }

    @Test
    public void isValidPearsonCorrelation_rejectsOutOfRangeValues() {
        // Constants.SMALL guards against tiny roundoff; well outside should fail.
        assertThat( CorrelationStats.isValidPearsonCorrelation( 1.5 ) ).isFalse();
        assertThat( CorrelationStats.isValidPearsonCorrelation( -1.5 ) ).isFalse();
    }

    @Test
    public void isValidPearsonCorrelation_acceptsTinyRoundoffOutsideRange() {
        // r within Constants.SMALL of the [-1, 1] bounds should still validate.
        assertThat( CorrelationStats.isValidPearsonCorrelation( 1.0 + Constants.SMALL / 2 ) ).isTrue();
        assertThat( CorrelationStats.isValidPearsonCorrelation( -1.0 - Constants.SMALL / 2 ) ).isTrue();
    }

    // ----------------------------------------------------------------------------------
    // pvalue edge cases
    // ----------------------------------------------------------------------------------

    @Test
    public void pvalue_atPerfectCorrelation_isZero() {
        assertThat( CorrelationStats.pvalue( 1.0, 10 ) ).isEqualTo( 0.0 );
        assertThat( CorrelationStats.pvalue( -1.0, 10 ) ).isEqualTo( 0.0 );
    }

    @Test
    public void pvalue_atZeroCorrelation_isOne() {
        assertThat( CorrelationStats.pvalue( 0.0, 10 ) ).isEqualTo( 1.0 );
    }

    @Test
    public void pvalue_insufficientDof_isOne() {
        // dof = count - 2; count <= 2 => dof <= 0 => return 1.0
        assertThat( CorrelationStats.pvalue( 0.5, 2 ) ).isEqualTo( 1.0 );
        assertThat( CorrelationStats.pvalue( 0.5, 1 ) ).isEqualTo( 1.0 );
    }

    @Test
    public void correlationTstat_atZero_isZero() {
        assertThat( CorrelationStats.correlationTstat( 0.0, 10 ) ).isEqualTo( 0.0 );
    }

    @Test
    public void correlationTstat_sign_matchesCorrelationSign() {
        assertThat( CorrelationStats.correlationTstat( 0.5, 10 ) ).isPositive();
        assertThat( CorrelationStats.correlationTstat( -0.5, 10 ) ).isNegative();
    }
}
