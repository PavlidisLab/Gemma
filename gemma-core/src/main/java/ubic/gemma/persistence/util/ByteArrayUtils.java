package ubic.gemma.persistence.util;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for working with byte arrays.
 *
 * @author poirigui
 * @see ByteArrayConverter
 */
public class ByteArrayUtils {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    public static byte[] floatArrayToBytes( float[] data ) {
        return byteArrayConverter.floatArrayToBytes( data );
    }

    public static float[] byteArrayToFloats( byte[] bytes ) {
        return byteArrayConverter.byteArrayToFloats( bytes );
    }

    public static byte[] doubleArrayToBytes( double[] data ) {
        return byteArrayConverter.doubleArrayToBytes( data );
    }

    public static double[] byteArrayToDoubles( byte[] bytes ) {
        return byteArrayConverter.byteArrayToDoubles( bytes );
    }

    public static byte[] intArrayToBytes( int[] data ) {
        return byteArrayConverter.intArrayToBytes( data );
    }

    public static int[] byteArrayToInts( byte[] bytes ) {
        return byteArrayConverter.byteArrayToInts( bytes );
    }

    public static byte[] longArrayToBytes( long[] data ) {
        return byteArrayConverter.longArrayToBytes( data );
    }

    public static long[] byteArrayToLongs( byte[] bytes ) {
        return byteArrayConverter.byteArrayToLongs( bytes );
    }

    public static char[] byteArrayToChars( byte[] barray ) {
        return byteArrayConverter.byteArrayToChars( barray );
    }

    public static byte[] charArrayToBytes( char[] data ) {
        return byteArrayConverter.charArrayToBytes( data );
    }

    public static String[] byteArrayToStrings( byte[] bytes ) {
        return byteArrayToStrings( bytes, StandardCharsets.UTF_8 );
    }

    public static String[] byteArrayToStrings( byte[] bytes, Charset charset ) {
        return byteArrayConverter.byteArrayToStrings( bytes, charset );
    }

    public static byte[] stringsToByteArray( String[] data ) {
        return stringsToByteArray( data, StandardCharsets.UTF_8 );
    }

    public static byte[] stringsToByteArray( String[] data, Charset charset ) {
        return byteArrayConverter.stringArrayToBytes( data, charset );
    }

    public static byte[] stringsToTabbedBytes( String[] data ) {
        return stringsToTabbedBytes( data, StandardCharsets.UTF_8 );
    }

    public static byte[] stringsToTabbedBytes( String[] data, Charset charset ) {
        return byteArrayConverter.stringArrayToTabbedBytes( data, charset );
    }

    public static String[] byteArrayToTabbedStrings( byte[] bytes ) {
        return byteArrayToTabbedStrings( bytes, StandardCharsets.UTF_8 );
    }

    public static String[] byteArrayToTabbedStrings( byte[] bytes, Charset charset ) {
        return byteArrayConverter.byteArrayToTabbedStrings( bytes, charset );
    }

    public static boolean[] byteArrayToBooleans( byte[] bytes ) {
        return byteArrayConverter.byteArrayToBooleans( bytes );
    }

    public static byte[] booleanArrayToBytes( boolean[] data ) {
        return byteArrayConverter.booleanArrayToBytes( data );
    }

    public static byte[] doubleMatrixToBytes( double[][] matrix ) {
        return byteArrayConverter.doubleMatrixToBytes( matrix );
    }

    public static double[][] bytesToDoubleMatrix( byte[] bytes, int n ) {
        return byteArrayConverter.byteArrayToDoubleMatrix( bytes, n );
    }

    /**
     * Encode a square, exactly symmetric matrix as its lower triangle, diagonal included.
     * <p>
     * The layout is row-major over the lower triangle: row 0 contributes {@code m[0][0]}, row 1 contributes
     * {@code m[1][0], m[1][1]}, and row {@code i} contributes {@code m[i][0..i]}, so element {@code (i, j)} with
     * {@code j <= i} sits at double offset {@code i * (i + 1) / 2 + j}. Each value is an 8-byte big-endian IEEE 754
     * double, the same encoding {@link #doubleMatrixToBytes(double[][])} uses; the result is {@code n * (n + 1) / 2 * 8}
     * bytes, about half the full matrix. Read it back with {@link #bytesToSymmetricMatrix(byte[], int)}.
     * <p>
     * Symmetry is checked on the raw bit patterns ({@link Double#doubleToRawLongBits(double)}) before anything is
     * written, so a matrix whose upper triangle differs from its lower one anywhere, including in where its NaNs are
     * or in the sign of a zero, is refused rather than having half of it silently dropped.
     *
     * @param matrix a square matrix with {@code matrix[i][j]} bit-identical to {@code matrix[j][i]}
     * @return the packed lower triangle
     * @throws IllegalArgumentException if the matrix is not square or not exactly symmetric, or is too large to fit in
     *                                  a single byte array
     */
    public static byte[] symmetricMatrixToLowerTriangleBytes( double[][] matrix ) {
        int n = matrix.length;
        for ( int i = 0; i < n; i++ ) {
            if ( matrix[i].length != n ) {
                throw new IllegalArgumentException( String.format( "Matrix is not square: row %d has %d columns, expected %d.",
                        i, matrix[i].length, n ) );
            }
        }
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j < i; j++ ) {
                if ( Double.doubleToRawLongBits( matrix[i][j] ) != Double.doubleToRawLongBits( matrix[j][i] ) ) {
                    throw new IllegalArgumentException( String.format( "Matrix is not exactly symmetric: [%d][%d] = %s but [%d][%d] = %s.",
                            i, j, matrix[i][j], j, i, matrix[j][i] ) );
                }
            }
        }
        long numBytes = lowerTriangleSize( n ) * Double.BYTES;
        if ( numBytes > Integer.MAX_VALUE - 8 ) {
            throw new IllegalArgumentException( String.format( "The lower triangle of a %d x %d matrix (%d bytes) does not fit in a byte array.",
                    n, n, numBytes ) );
        }
        byte[] bytes = new byte[( int ) numBytes];
        DoubleBuffer buf = ByteBuffer.wrap( bytes ).asDoubleBuffer();
        for ( int i = 0; i < n; i++ ) {
            buf.put( matrix[i], 0, i + 1 );
        }
        return bytes;
    }

    /**
     * Decode a symmetric {@code n x n} matrix stored either in full or as its lower triangle.
     * <p>
     * The format is decided by length alone: {@code n * n * 8} bytes is the full row-major matrix written by
     * {@link #doubleMatrixToBytes(double[][])} and is decoded exactly as {@link #bytesToDoubleMatrix(byte[], int)}
     * decodes it, and {@code n * (n + 1) / 2 * 8} bytes is the lower triangle written by
     * {@link #symmetricMatrixToLowerTriangleBytes(double[][])}, mirrored into the upper one. The two lengths differ for
     * every {@code n > 1}; for {@code n = 1} they coincide and so do the two encodings.
     *
     * @param bytes the stored matrix
     * @param n     the number of rows and columns, known from elsewhere
     * @return a new {@code n x n} matrix
     * @throws IllegalArgumentException if {@code n} is not positive, or the length matches neither format
     */
    public static double[][] bytesToSymmetricMatrix( byte[] bytes, int n ) {
        if ( n <= 0 ) {
            throw new IllegalArgumentException( "The matrix dimension must be positive, got " + n + "." );
        }
        long fullBytes = ( long ) n * n * Double.BYTES;
        long triangleBytes = lowerTriangleSize( n ) * Double.BYTES;
        if ( bytes.length == fullBytes ) {
            return bytesToDoubleMatrix( bytes, n );
        } else if ( bytes.length == triangleBytes ) {
            double[][] matrix = new double[n][n];
            DoubleBuffer buf = ByteBuffer.wrap( bytes ).asDoubleBuffer();
            for ( int i = 0; i < n; i++ ) {
                buf.get( matrix[i], 0, i + 1 );
                for ( int j = 0; j < i; j++ ) {
                    matrix[j][i] = matrix[i][j];
                }
            }
            return matrix;
        } else {
            throw new IllegalArgumentException( String.format( "A symmetric %d x %d matrix is stored in %d bytes (full) or %d bytes (lower triangle), but got %d bytes.",
                    n, n, fullBytes, triangleBytes, bytes.length ) );
        }
    }

    private static long lowerTriangleSize( int n ) {
        return ( long ) n * ( n + 1 ) / 2;
    }

    public static <T> byte[] objectArrayToBytes( T[] objects ) {
        return byteArrayConverter.objectArrayToBytes( objects, StandardCharsets.UTF_8 );
    }

    public static <T> T[] byteArrayToObjects( byte[] bytes, Class<T> type ) {
        return byteArrayConverter.byteArrayToObjects( bytes, type, StandardCharsets.UTF_8 );
    }
}
