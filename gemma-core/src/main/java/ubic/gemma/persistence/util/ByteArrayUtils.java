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
}
