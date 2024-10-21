package ubic.gemma.persistence.util;

import ubic.basecode.io.ByteArrayConverter;

/**
 * Utilities for working with byte arrays.
 *
 * @author poirigui
 * @see ByteArrayConverter
 */
public class ByteArrayUtils {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    public static byte[] doubleArrayToBytes( Double[] data ) {
        return byteArrayConverter.doubleArrayToBytes( data );
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

    public static long[] byteArrayToLongs( byte[] bytes ) {
        return byteArrayConverter.byteArrayToLongs( bytes );
    }

    public static char[] byteArrayToChars( byte[] bytes ) {
        return byteArrayConverter.byteArrayToChars( bytes );
    }

    public static String[] byteArrayToStrings( byte[] bytes ) {
        return byteArrayConverter.byteArrayToStrings( bytes );
    }

    public static String byteArrayToAsciiString( byte[] bytes ) {
        return byteArrayConverter.byteArrayToAsciiString( bytes );
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

    public static byte[] toBytes( Object[] objects ) {
        return byteArrayConverter.toBytes( objects );
    }
}
