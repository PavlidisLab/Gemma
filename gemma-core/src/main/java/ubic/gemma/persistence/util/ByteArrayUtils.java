package ubic.gemma.persistence.util;

import ubic.basecode.io.ByteArrayConverter;

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
        return byteArrayConverter.byteArrayToStrings( bytes, StandardCharsets.UTF_8 );
    }

    public static byte[] stringsToByteArray( String[] data ) {
        return byteArrayConverter.stringArrayToBytes( data, StandardCharsets.UTF_8 );
    }

    public static byte[] stringsToTabbedBytes( String[] data ) {
        return byteArrayConverter.stringArrayToTabbedBytes( data, StandardCharsets.UTF_8 );
    }

    public static String[] byteArrayToTabbedStrings( byte[] bytes ) {
        return byteArrayConverter.byteArrayToTabbedStrings( bytes, StandardCharsets.UTF_8 );
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

    public static byte[] objectArrayToBytes( Object[] objects ) {
        return byteArrayConverter.objectArrayToBytes( objects, StandardCharsets.UTF_8 );
    }

    public static <T> T[] byteArrayToObjects( byte[] bytes, Class<T> type ) {
        return byteArrayConverter.byteArrayToObjects( bytes, type, StandardCharsets.UTF_8 );
    }
}
