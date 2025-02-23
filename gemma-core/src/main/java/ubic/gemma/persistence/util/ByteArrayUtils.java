package ubic.gemma.persistence.util;

import ubic.basecode.io.ByteArrayConverter;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;

/**
 * Utilities for working with byte arrays.
 *
 * @author poirigui
 * @see ByteArrayConverter
 */
public class ByteArrayUtils {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    public static byte[] floatArrayToBytes( float[] darray ) {
        if ( darray == null ) {
            return null;
        }
        FloatBuffer.wrap( darray );
        ByteBuffer buffer = ByteBuffer.allocate( 4 * darray.length );
        for ( float d : darray ) {
            buffer.putFloat( d );
        }
        return buffer.array();
    }

    public static float[] byteArrayToFloats( byte[] barray ) {
        if ( barray == null ) return null;
        FloatBuffer buf = ByteBuffer.wrap( barray ).asFloatBuffer();
        float[] array = new float[buf.remaining()];
        buf.get( array );
        return array;
    }

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

    public static byte[] longArrayToBytes( long[] data ) {
        return byteArrayConverter.longArrayToBytes( data );
    }

    public static long[] byteArrayToLongs( byte[] bytes ) {
        return byteArrayConverter.byteArrayToLongs( bytes );
    }

    public static char[] byteArrayToChars( byte[] barray ) {
        // TODO: upstream the fix to baseCode
        if ( barray == null ) return null;
        CharBuffer buf = ByteBuffer.wrap( barray ).asCharBuffer();
        char[] array = new char[buf.remaining()];
        buf.get( array );
        return array;
    }

    public static byte[] charArrayToBytes( char[] data ) {
        return byteArrayConverter.charArrayToBytes( data );
    }

    public static String[] byteArrayToStrings( byte[] bytes ) {
        return byteArrayConverter.byteArrayToStrings( bytes );
    }

    public static byte[] stringsToByteArray( String[] data ) {
        return byteArrayConverter.stringArrayToBytes( data );
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
