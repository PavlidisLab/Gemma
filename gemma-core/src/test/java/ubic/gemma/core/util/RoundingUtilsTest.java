package ubic.gemma.core.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RoundingUtils}, the JSON-side counterpart of {@link TsvUtils#format(double)}.
 *
 * @author paul
 */
public class RoundingUtilsTest {

    @Test
    public void testAlreadyShorterThanFourDigitsIsUnchanged() {
        assertThat( RoundingUtils.round( 2.5 ) ).isEqualTo( 2.5 );
        assertThat( RoundingUtils.round( 1.0 ) ).isEqualTo( 1.0 );
        assertThat( RoundingUtils.round( 0.001 ) ).isEqualTo( 0.001 );
    }

    @Test
    public void testZeroKeepsItsSign() {
        assertThat( RoundingUtils.round( 0.0 ) ).isEqualTo( 0.0 );
        // -0.0 == 0.0, so isEqualTo would pass either way; compare the bits.
        assertThat( Double.doubleToRawLongBits( RoundingUtils.round( -0.0 ) ) )
                .isEqualTo( Double.doubleToRawLongBits( -0.0 ) );
    }

    @Test
    public void testNegativeIsRoundedNotTruncated() {
        assertThat( RoundingUtils.round( -7.6075330481152580 ) ).isEqualTo( -7.608 );
        assertThat( RoundingUtils.round( -0.000123456789 ) ).isEqualTo( -1.235E-4 );
    }

    /**
     * Math.round(NaN) is 0.0, which on a mean-variance or correlation payload reads as "this value is zero"
     * rather than "not known". The infinities have the same hazard in the other direction.
     */
    @Test
    public void testNonFiniteValuesAreLeftAlone() {
        assertThat( RoundingUtils.round( Double.NaN ) ).isNaN();
        assertThat( RoundingUtils.round( Double.POSITIVE_INFINITY ) ).isEqualTo( Double.POSITIVE_INFINITY );
        assertThat( RoundingUtils.round( Double.NEGATIVE_INFINITY ) ).isEqualTo( Double.NEGATIVE_INFINITY );
    }

    /**
     * The reason the rule is significant digits and not decimal places: a corrected p-value of 1e-300 has to
     * come back as 1e-300, and any fixed number of decimals flattens it to zero.
     */
    @Test
    public void testVerySmallValuesSurvive() {
        assertThat( RoundingUtils.round( 1e-300 ) ).isEqualTo( 1e-300 );
        assertThat( RoundingUtils.round( 1.23456789e-300 ) ).isEqualTo( 1.235e-300 );
        assertThat( RoundingUtils.round( Double.MIN_VALUE ) ).isEqualTo( Double.MIN_VALUE );
    }

    @Test
    public void testLargeValuesKeepTheirMagnitude() {
        assertThat( RoundingUtils.round( 123456789.123 ) ).isEqualTo( 1.235E8 );
        assertThat( RoundingUtils.round( 1.23456789e300 ) ).isEqualTo( 1.235e300 );
    }

    /**
     * The point of the exercise: four significant digits has to actually shorten the serialized form, or the
     * bytes on the wire do not move.
     */
    @Test
    public void testRoundedValueSerializesShort() {
        assertThat( Double.toString( RoundingUtils.round( 7.607533048115258 ) ) ).isEqualTo( "7.608" );
        assertThat( Double.toString( RoundingUtils.round( 0.1234567890123 ) ) ).isEqualTo( "0.1235" );
    }

    @Test
    public void testBoxedOverloadPassesNullThrough() {
        assertThat( RoundingUtils.round( ( Double ) null ) ).isNull();
        assertThat( RoundingUtils.round( Double.valueOf( 3.14159265358979 ) ) ).isEqualTo( 3.142 );
    }

    /**
     * Rounding in place would corrupt the loaded entity's array or the cached matrix's backing store, which
     * is the hazard the sample-correlation rounding was written around.
     */
    @Test
    public void testArrayIsCopiedNotRoundedInPlace() {
        double[] src = { 7.607533048115258, Double.NaN, 0.0, -1e-300 };
        double[] out = RoundingUtils.roundedCopy( src );
        assertThat( out ).isNotSameAs( src );
        assertThat( src[0] ).isEqualTo( 7.607533048115258 );
        assertThat( out[0] ).isEqualTo( 7.608 );
        assertThat( out[1] ).isNaN();
        assertThat( out[2] ).isEqualTo( 0.0 );
        assertThat( out[3] ).isEqualTo( -1e-300 );
    }

    @Test
    public void testMatrixIsCopiedRowByRow() {
        double[][] src = { { 0.123456789, 1.0 }, { Double.POSITIVE_INFINITY, -0.987654321 } };
        double[][] out = RoundingUtils.roundedCopy( src );
        assertThat( out ).isNotSameAs( src );
        assertThat( out[0] ).isNotSameAs( src[0] );
        assertThat( src[0][0] ).isEqualTo( 0.123456789 );
        assertThat( out[0][0] ).isEqualTo( 0.1235 );
        assertThat( out[1][0] ).isEqualTo( Double.POSITIVE_INFINITY );
        assertThat( out[1][1] ).isEqualTo( -0.9877 );
    }

    @Test
    public void testNullArraysArePassedThrough() {
        assertThat( RoundingUtils.roundedCopy( ( double[] ) null ) ).isNull();
        assertThat( RoundingUtils.roundedCopy( ( double[][] ) null ) ).isNull();
    }

    @Test
    public void testSerializerEmitsRoundedNumberAndSkipsNull() throws Exception {
        Holder h = new Holder();
        h.value = 3.14159265358979;
        h.other = null;
        assertThat( new ObjectMapper().writeValueAsString( h ) ).isEqualTo( "{\"value\":3.142}" );
    }

    @Test
    public void testSerializerLeavesTheInMemoryValueExact() throws Exception {
        Holder h = new Holder();
        h.value = 1.23456789e-300;
        new ObjectMapper().writeValueAsString( h );
        assertThat( h.value ).isEqualTo( 1.23456789e-300 );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Holder {
        @JsonSerialize(using = RoundingUtils.SignificantDigitsSerializer.class)
        public Double value;
        @JsonSerialize(using = RoundingUtils.SignificantDigitsSerializer.class)
        public Double other;
    }
}
