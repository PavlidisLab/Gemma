package ubic.gemma.persistence.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    public void symmetricMatrix_roundTripsBitForBit() {
        for ( int n : new int[] { 1, 2, 3, 59, 500 } ) {
            double[][] in = randomSymmetric( n, new Random( n ) );
            byte[] bytes = ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( in );
            assertThat( bytes ).as( "n = %d", n ).hasSize( n * ( n + 1 ) / 2 * 8 );
            assertBitIdentical( ByteArrayUtils.bytesToSymmetricMatrix( bytes, n ), in );
        }
    }

    /**
     * A row written before the lower-triangle format holds the full matrix, and must read exactly as the old reader
     * read it.
     */
    @Test
    public void symmetricMatrix_readsTheFullFormatExactlyAsTheOldReaderDid() {
        for ( int n : new int[] { 1, 2, 3, 59, 500 } ) {
            double[][] in = randomSymmetric( n, new Random( 31L * n ) );
            byte[] full = ByteArrayUtils.doubleMatrixToBytes( in );
            double[][] old = ByteArrayUtils.bytesToDoubleMatrix( full, n );
            double[][] now = ByteArrayUtils.bytesToSymmetricMatrix( full, n );
            assertBitIdentical( now, old );
            assertBitIdentical( now, in );
        }
    }

    /**
     * The old reader never looked at symmetry, so a full-format row is decoded as stored even if it is not
     * symmetric: nothing about an existing row changes.
     */
    @Test
    public void symmetricMatrix_fullFormatIsNotMirrored() {
        double[][] in = { { 1, 2 }, { 3, 4 } };
        assertBitIdentical( ByteArrayUtils.bytesToSymmetricMatrix( ByteArrayUtils.doubleMatrixToBytes( in ), 2 ), in );
    }

    @Test
    public void symmetricMatrix_layoutIsTheLowerTriangleRowByRow() {
        double[][] in = {
                { 1, 2, 4 },
                { 2, 3, 5 },
                { 4, 5, 6 }
        };
        assertThat( ByteArrayUtils.byteArrayToDoubles( ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( in ) ) )
                .containsExactly( 1, 2, 3, 4, 5, 6 );
    }

    @Test
    public void symmetricMatrix_lengthMatchingNeitherFormatThrows() {
        double[][] in = randomSymmetric( 5, new Random( 5 ) );
        byte[] triangle = ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( in );
        byte[] full = ByteArrayUtils.doubleMatrixToBytes( in );
        // one double short of each, one double over, a stray byte, empty, and a matrix for a different n
        for ( int length : new int[] { triangle.length - 8, triangle.length + 8, full.length - 8, full.length + 8, triangle.length + 1, 0 } ) {
            assertThatThrownBy( () -> ByteArrayUtils.bytesToSymmetricMatrix( new byte[length], 5 ) )
                    .as( "%d bytes", length )
                    .isInstanceOf( IllegalArgumentException.class );
        }
        assertThatThrownBy( () -> ByteArrayUtils.bytesToSymmetricMatrix( triangle, 4 ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ByteArrayUtils.bytesToSymmetricMatrix( triangle, 0 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void symmetricMatrix_refusesAMatrixThatIsNotExactlySymmetric() {
        assertThatThrownBy( () -> ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( new double[][] { { 1, 2 }, { 2.0000000000000004, 1 } } ) )
                .isInstanceOf( IllegalArgumentException.class );
        // NaN on one side only
        assertThatThrownBy( () -> ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( new double[][] { { 1, Double.NaN }, { 0.5, 1 } } ) )
                .isInstanceOf( IllegalArgumentException.class );
        // the two zeros compare equal as doubles but are different values
        assertThatThrownBy( () -> ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( new double[][] { { 1, 0.0 }, { -0.0, 1 } } ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ByteArrayUtils.symmetricMatrixToLowerTriangleBytes( new double[][] { { 1, 2 }, { 2 } } ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    /**
     * A symmetric matrix with whole NaN rows and columns, as an empty pseudo-bulk sample produces, plus the special
     * values a double can take: -0.0, the infinities, subnormals and a NaN with a non-canonical payload.
     */
    private static double[][] randomSymmetric( int n, Random random ) {
        double[] specials = { -0.0, 0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE,
                -Double.MIN_VALUE, Double.MIN_NORMAL / 3, Double.MAX_VALUE, Double.NaN,
                Double.longBitsToDouble( 0x7ff8_0000_dead_beefL ) };
        double[][] m = new double[n][n];
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j <= i; j++ ) {
                double v = random.nextInt( 10 ) == 0 ? specials[random.nextInt( specials.length )] : 2 * random.nextDouble() - 1;
                m[i][j] = v;
                m[j][i] = v;
            }
        }
        if ( n > 2 ) {
            for ( int k : new int[] { 1, n - 1 } ) {
                for ( int j = 0; j < n; j++ ) {
                    m[k][j] = Double.NaN;
                    m[j][k] = Double.NaN;
                }
            }
        }
        return m;
    }

    private static void assertBitIdentical( double[][] actual, double[][] expected ) {
        assertThat( actual.length ).isEqualTo( expected.length );
        for ( int i = 0; i < expected.length; i++ ) {
            assertThat( actual[i].length ).isEqualTo( expected[i].length );
            for ( int j = 0; j < expected[i].length; j++ ) {
                assertThat( Double.doubleToRawLongBits( actual[i][j] ) )
                        .as( "[%d][%d]: %s vs %s", i, j, actual[i][j], expected[i][j] )
                        .isEqualTo( Double.doubleToRawLongBits( expected[i][j] ) );
            }
        }
    }
}
