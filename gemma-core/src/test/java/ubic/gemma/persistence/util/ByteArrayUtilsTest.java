package ubic.gemma.persistence.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip + edge-case unit tests for {@link ByteArrayUtils}, the static facade used
 * across the persistence layer to encode/decode primitive arrays into the BLOB columns
 * that store expression-data vectors. The class is a thin delegating wrapper around
 * {@code ByteArrayConverter}; these tests pin the round-trip invariants so a future
 * change to either side surfaces immediately.
 *
 * @author claude
 */
public class ByteArrayUtilsTest {

    @Test
    public void doubleArrayRoundTrip_preservesValuesInOrder() {
        double[] in = { 0.0, -1.5, 3.14159, Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN, Double.POSITIVE_INFINITY };
        byte[] bytes = ByteArrayUtils.doubleArrayToBytes( in );
        double[] out = ByteArrayUtils.byteArrayToDoubles( bytes );

        assertThat( out ).hasSize( in.length );
        // NaN does not equal itself; compare bitwise
        for ( int i = 0; i < in.length; i++ ) {
            assertThat( Double.doubleToRawLongBits( out[i] ) )
                    .as( "double[%d]", i )
                    .isEqualTo( Double.doubleToRawLongBits( in[i] ) );
        }
        // every double encodes to 8 bytes
        assertThat( bytes ).hasSize( in.length * 8 );
    }

    @Test
    public void floatArrayRoundTrip_preservesValuesAndSize() {
        float[] in = { 0f, -1.5f, 3.14f, Float.MAX_VALUE };
        byte[] bytes = ByteArrayUtils.floatArrayToBytes( in );
        float[] out = ByteArrayUtils.byteArrayToFloats( bytes );
        assertThat( out ).containsExactly( in );
        assertThat( bytes ).hasSize( in.length * 4 );
    }

    @Test
    public void intArrayRoundTrip_preservesValuesAndSize() {
        int[] in = { 0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 42 };
        byte[] bytes = ByteArrayUtils.intArrayToBytes( in );
        int[] out = ByteArrayUtils.byteArrayToInts( bytes );
        assertThat( out ).containsExactly( in );
        assertThat( bytes ).hasSize( in.length * 4 );
    }

    @Test
    public void longArrayRoundTrip_preservesValuesAndSize() {
        long[] in = { 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE };
        byte[] bytes = ByteArrayUtils.longArrayToBytes( in );
        long[] out = ByteArrayUtils.byteArrayToLongs( bytes );
        assertThat( out ).containsExactly( in );
        assertThat( bytes ).hasSize( in.length * 8 );
    }

    @Test
    public void charArrayRoundTrip_preservesValues() {
        char[] in = { 'A', 'z', '\0', 'ÿ' };
        byte[] bytes = ByteArrayUtils.charArrayToBytes( in );
        char[] out = ByteArrayUtils.byteArrayToChars( bytes );
        assertThat( out ).containsExactly( in );
    }

    @Test
    public void booleanArrayRoundTrip_preservesValues() {
        boolean[] in = { true, false, false, true, true };
        byte[] bytes = ByteArrayUtils.booleanArrayToBytes( in );
        boolean[] out = ByteArrayUtils.byteArrayToBooleans( bytes );
        assertThat( out ).containsExactly( in );
    }

    @Test
    public void emptyDoubleArray_roundTrips() {
        double[] in = new double[0];
        byte[] bytes = ByteArrayUtils.doubleArrayToBytes( in );
        double[] out = ByteArrayUtils.byteArrayToDoubles( bytes );
        assertThat( out ).isEmpty();
        assertThat( bytes ).isEmpty();
    }

    @Test
    public void stringArrayRoundTrip_utf8_default() {
        String[] in = { "hello", "world", "" };
        byte[] bytes = ByteArrayUtils.stringsToByteArray( in );
        String[] out = ByteArrayUtils.byteArrayToStrings( bytes );
        assertThat( out ).containsExactly( in );
    }

    @Test
    public void stringArrayRoundTrip_explicitCharset_preservesNonAscii() {
        String[] in = { "héllo", "wörld", "数据" };
        byte[] bytes = ByteArrayUtils.stringsToByteArray( in, StandardCharsets.UTF_8 );
        String[] out = ByteArrayUtils.byteArrayToStrings( bytes, StandardCharsets.UTF_8 );
        assertThat( out ).containsExactly( in );
    }

    @Test
    public void tabbedStringRoundTrip_preservesTokens() {
        String[] in = { "a", "b", "cd" };
        byte[] bytes = ByteArrayUtils.stringsToTabbedBytes( in );
        String[] out = ByteArrayUtils.byteArrayToTabbedStrings( bytes );
        assertThat( out ).containsExactly( in );
    }

    @Test
    public void doubleMatrixRoundTrip_preservesShape() {
        double[][] in = {
                { 1.0, 2.0, 3.0 },
                { 4.0, 5.0, 6.0 },
                { 7.0, 8.0, 9.0 }
        };
        byte[] bytes = ByteArrayUtils.doubleMatrixToBytes( in );
        double[][] out = ByteArrayUtils.bytesToDoubleMatrix( bytes, in[0].length );
        assertThat( out.length ).isEqualTo( in.length );
        for ( int i = 0; i < in.length; i++ ) {
            assertThat( out[i] ).containsExactly( in[i] );
        }
    }
}
