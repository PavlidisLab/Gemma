/*
 * The baseCode project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to convert byte arrays (e.g., Blobs) to and from other types of arrays.
 * <p>
 * Ported in-tree from {@code ubic.basecode.io.ByteArrayConverter} as part of the baseCode in-tree
 * migration; consumed by {@link ByteArrayUtils}.
 *
 * @author Kiran Keshav
 * @author Paul Pavlidis
 */
public final class ByteArrayConverter {

    // sizes are in bytes.
    // TODO: these could be static methods.

    private static final int DOUBLE_SIZE = 8;

    public byte[] booleanArrayToBytes( boolean[] boolarray ) {
        if ( boolarray == null ) return null;
        ByteBuffer buffer = ByteBuffer.allocate( boolarray.length );
        for ( boolean b : boolarray ) {
            buffer.put( b ? ( byte ) 1 : ( byte ) 0 );
        }
        return buffer.array();
    }

    public boolean[] byteArrayToBooleans( byte[] barray ) {
        if ( barray == null ) return null;
        boolean[] iarray = new boolean[barray.length];
        for ( int i = 0; i < barray.length; i++ ) {
            iarray[i] = barray[i] != 0;
        }
        return iarray;
    }

    public byte[] doubleArrayToBytes( double[] darray ) {
        if ( darray == null ) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate( 8 * darray.length );
        for ( double d : darray ) {
            buffer.putDouble( d );
        }
        return buffer.array();
    }

    public double[] byteArrayToDoubles( byte[] barray ) {
        if ( barray == null ) return null;
        DoubleBuffer buf = ByteBuffer.wrap( barray ).asDoubleBuffer();
        double[] array = new double[buf.remaining()];
        buf.get( array );
        return array;
    }

    public byte[] doubleMatrixToBytes( double[][] testm ) {
        if ( testm == null ) {
            return null;
        }
        if ( testm.length == 0 ) {
            return new byte[0];
        }
        int rowSize = testm[0].length;
        double[] a = new double[testm.length * rowSize];
        for ( int i = 0; i < testm.length; i++ ) {
            if ( testm[i].length != rowSize ) {
                throw new IllegalArgumentException( "Cannot serialize ragged matrix" );
            }
            System.arraycopy( testm[i], 0, a, rowSize * i, rowSize );
        }
        return doubleArrayToBytes( a );
    }

    /**
     * Convert a byte array to a double matrix, assuming it is square.
     */
    public double[][] byteArrayToDoubleMatrix( byte[] barray ) {
        if ( barray == null ) {
            return null;
        }
        int numDoubles = barray.length / DOUBLE_SIZE;
        int columns = ( int ) Math.sqrt( numDoubles );
        if ( columns * columns != barray.length / DOUBLE_SIZE ) {
            throw new IllegalArgumentException( "The byte array is not square." );
        }
        return byteArrayToDoubleMatrix( barray, columns );
    }

    /**
     * Convert a byte array to a double matrix.
     *
     * @param columns the number of columns in the matrix
     */
    public double[][] byteArrayToDoubleMatrix( byte[] barray, int columns ) throws IllegalArgumentException {
        if ( barray == null ) {
            return null;
        }
        if ( barray.length == 0 ) {
            return new double[0][columns];
        }
        int numDoubles = barray.length / DOUBLE_SIZE;
        if ( numDoubles % columns != 0 ) {
            throw new IllegalArgumentException( "The number of doubles in the byte array (" + numDoubles
                + ") does not divide evenly into the number of items expected per row (" + columns + ")." );
        }
        int numRows = numDoubles / columns;
        double[][] answer = new double[numRows][];
        byte[] row = new byte[columns * DOUBLE_SIZE];
        int bytesPerRow = columns * DOUBLE_SIZE;
        for ( int rownum = 0; rownum < numRows; rownum++ ) {
            System.arraycopy( barray, rownum * bytesPerRow, row, 0, bytesPerRow );
            answer[rownum] = byteArrayToDoubles( row );
        }
        return answer;
    }

    /**
     * Note that this method cannot differentiate between empty strings and null strings. A string that is empty will be
     * returned as an empty string, not null, while a null string will be stored as an empty string.
     *
     * @param charset charset to use when converting strings to bytes
     */
    public byte[] stringArrayToBytes( String[] stringArray, Charset charset ) {
        if ( stringArray == null ) return null;
        int size = 0;
        for ( String element : stringArray ) {
            size += element != null ? element.getBytes( charset ).length : 0;
            size += 1;
        }
        ByteBuffer buffer = ByteBuffer.allocate( size );
        for ( String element : stringArray ) {
            if ( element != null ) {
                buffer.put( element.getBytes( charset ) );
            }
            buffer.put( ( byte ) 0 );
        }
        return buffer.array();
    }

    /**
     * Convert a byte array into a array of Strings.
     * <p>
     * It is assumed that separate strings are delimited by a NUL character. Note that this method cannot
     * differentiate between empty strings and null strings. A string that is empty will be returned as an empty string,
     * not null.
     *
     * @param charset charset to use when decoding bytes into strings
     */
    public String[] byteArrayToStrings( byte[] bytes, Charset charset ) {
        if ( bytes == null ) {
            return null;
        }
        List<String> strings = new ArrayList<>();
        int len = 0;
        for ( int i = 0; i < bytes.length; i++ ) {
            byte element = bytes[i];
            if ( element == 0 ) {
                strings.add( new String( bytes, i - len, len, charset ) );
                len = 0;
            } else {
                len++;
            }
        }
        return strings.toArray( new String[0] );
    }

    /**
     * Convert an array of strings to a byte array where the delimiter is a tab character.
     * <p>
     * If the string contains actual {@code \t} characters, they are escaped as {@code \\t}. Note that those will be
     * decoded as escaped tabs by {@link #byteArrayToTabbedStrings(byte[], Charset)}.
     * <p>
     * This method does not distinguish between null and empty strings. Those will be decoded as empty strings by
     * {@link #byteArrayToTabbedStrings(byte[], Charset)}.
     */
    public byte[] stringArrayToTabbedBytes( String[] strings, Charset charset ) {
        if ( strings == null ) return null;
        String[] escapedStrings = new String[strings.length];
        for ( int i = 0; i < strings.length; i++ ) {
            escapedStrings[i] = formatAsTabbedString( strings[i] );
        }
        return StringUtils.join( escapedStrings, '\t' ).getBytes( charset );
    }

    public String[] byteArrayToTabbedStrings( byte[] bytes, Charset charset ) {
        if ( bytes == null ) return null;
        return StringUtils.splitPreserveAllTokens( new String( bytes, charset ), '\t' );
    }

    /**
     * Convert a byte array to a tab-delimited string.
     *
     * @param type    The Class of primitives the bytes are to be interpreted as. If this is String, then the bytes are
     *                directly interpreted as tab-delimited string (e.g., no extra tabs are added).
     * @param charset charset to use when decoding bytes into strings
     * @throws UnsupportedOperationException if Class is a type that can't be converted by this.
     */
    public String byteArrayToTabbedString( byte[] bytes, Class<?> type, Charset charset ) {
        if ( bytes == null ) return null;
        if ( type.equals( Float.class ) ) {
            Float[] array = ArrayUtils.toObject( byteArrayToFloats( bytes ) );
            return formatAsTabbedString( array );
        } else if ( type.equals( Double.class ) ) {
            Double[] array = ArrayUtils.toObject( byteArrayToDoubles( bytes ) );
            return formatAsTabbedString( array );
        } else if ( type.equals( Integer.class ) ) {
            Integer[] array = ArrayUtils.toObject( byteArrayToInts( bytes ) );
            return formatAsTabbedString( array );
        } else if ( type.equals( Long.class ) ) {
            Long[] array = ArrayUtils.toObject( byteArrayToLongs( bytes ) );
            return formatAsTabbedString( array );
        } else if ( type.equals( String.class ) ) {
            return new String( bytes, charset );
        } else if ( type.equals( Boolean.class ) ) {
            Boolean[] array = ArrayUtils.toObject( byteArrayToBooleans( bytes ) );
            return formatAsTabbedString( array );
        } else if ( type.equals( Character.class ) ) {
            Character[] array = ArrayUtils.toObject( byteArrayToChars( bytes ) );
            return formatAsTabbedString( array );
        } else {
            throw new UnsupportedOperationException( "Can't convert " + type.getName() );
        }
    }

    private String formatAsTabbedString( Object[] array ) {
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < array.length; i++ ) {
            buf.append( formatAsTabbedString( array[i] ) );
            if ( i != array.length - 1 ) buf.append( "\t" ); // so we don't have a trailing tab.
        }
        return buf.toString();
    }

    private String formatAsTabbedString( Object object ) {
        if ( object == null ) {
            return "";
        }
        return String.valueOf( object )
            .replace( "\t", "\\t" );
    }

    public byte[] charArrayToBytes( char[] carray ) {
        if ( carray == null ) return null;
        ByteBuffer buffer = ByteBuffer.allocate( 2 * carray.length );
        for ( char c : carray ) {
            buffer.putChar( c );
        }
        return buffer.array();
    }

    public char[] byteArrayToChars( byte[] barray ) {
        if ( barray == null ) return null;
        CharBuffer buf = ByteBuffer.wrap( barray ).asCharBuffer();
        char[] array = new char[buf.remaining()];
        buf.get( array );
        return array;
    }

    public byte[] floatArrayToBytes( float[] darray ) {
        if ( darray == null ) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate( 4 * darray.length );
        for ( float d : darray ) {
            buffer.putFloat( d );
        }
        return buffer.array();
    }

    public float[] byteArrayToFloats( byte[] barray ) {
        if ( barray == null ) return null;
        FloatBuffer buf = ByteBuffer.wrap( barray ).asFloatBuffer();
        float[] array = new float[buf.remaining()];
        buf.get( array );
        return array;
    }

    public byte[] intArrayToBytes( int[] iarray ) {
        if ( iarray == null ) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate( 4 * iarray.length );
        for ( int i : iarray ) {
            buffer.putInt( i );
        }
        return buffer.array();
    }

    public int[] byteArrayToInts( byte[] barray ) {
        if ( barray == null ) return null;
        IntBuffer intBuf = ByteBuffer.wrap( barray ).asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get( array );
        return array;
    }

    public byte[] longArrayToBytes( long[] larray ) {
        if ( larray == null ) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate( 8 * larray.length );
        for ( long i : larray ) {
            buffer.putLong( i );
        }
        return buffer.array();
    }

    /**
     * @return long[] resulting from parse of the bytes.
     */
    public long[] byteArrayToLongs( byte[] barray ) {
        if ( barray == null ) return null;
        LongBuffer buf = ByteBuffer.wrap( barray ).asLongBuffer();
        long[] array = new long[buf.remaining()];
        buf.get( array );
        return array;
    }

    /**
     * Convert an array of Objects into an array of bytes.
     *
     * @param array   of objects to be converted to bytes.
     * @param charset charset to use when converting strings to bytes
     * @throws UnsupportedOperationException if Objects are a type that can't be converted by this.
     */
    public <T> byte[] objectArrayToBytes( T[] array, Charset charset ) {
        if ( array == null ) return null;
        if ( array instanceof Boolean[] ) {
            return booleanArrayToBytes( ArrayUtils.toPrimitive( ( Boolean[] ) array ) );
        } else if ( array instanceof Float[] ) {
            return floatArrayToBytes( ArrayUtils.toPrimitive( ( Float[] ) array ) );
        } else if ( array instanceof Double[] ) {
            return doubleArrayToBytes( ArrayUtils.toPrimitive( ( Double[] ) array ) );
        } else if ( array instanceof Character[] ) {
            return charArrayToBytes( ArrayUtils.toPrimitive( ( Character[] ) array ) );
        } else if ( array instanceof String[] ) {
            return stringArrayToBytes( ( String[] ) array, charset );
        } else if ( array instanceof Integer[] ) {
            return intArrayToBytes( ArrayUtils.toPrimitive( ( Integer[] ) array ) );
        } else if ( array instanceof Long[] ) {
            return longArrayToBytes( ArrayUtils.toPrimitive( ( Long[] ) array ) );
        } else if ( array.getClass().equals( Object[].class ) ) {
            // require a copy...
            if ( array.length != 0 ) {
                //noinspection unchecked
                T[] typedArray = ( T[] ) Array.newInstance( array[0].getClass(), array.length );
                System.arraycopy( array, 0, typedArray, 0, array.length );
                return objectArrayToBytes( typedArray, charset );
            } else {
                return new byte[0];
            }
        } else {
            throw new UnsupportedOperationException( "Can't convert " + array[0].getClass() + " to bytes" );
        }
    }

    /**
     * @param charset charset to use when decoding strings from bytes
     * @throws UnsupportedOperationException if type is a type that can't be converted by this.
     */
    @SuppressWarnings("unchecked")
    public <T> T[] byteArrayToObjects( byte[] barray, Class<T> type, Charset charset ) {
        if ( Boolean.class.isAssignableFrom( type ) ) {
            return ( T[] ) ArrayUtils.toObject( byteArrayToBooleans( barray ) );
        } else if ( Character.class.isAssignableFrom( type ) ) {
            return ( T[] ) ArrayUtils.toObject( byteArrayToChars( barray ) );
        } else if ( Float.class.isAssignableFrom( type ) ) {
            return ( T[] ) ArrayUtils.toObject( byteArrayToFloats( barray ) );
        } else if ( Double.class.isAssignableFrom( type ) ) {
            return ( T[] ) ArrayUtils.toObject( byteArrayToDoubles( barray ) );
        } else if ( Integer.class.isAssignableFrom( type ) ) {
            return ( T[] ) ArrayUtils.toObject( byteArrayToInts( barray ) );
        } else if ( Long.class.isAssignableFrom( type ) ) {
            return ( T[] ) ArrayUtils.toObject( byteArrayToLongs( barray ) );
        } else if ( String.class.isAssignableFrom( type ) ) {
            return ( T[] ) byteArrayToStrings( barray, charset );
        } else {
            throw new UnsupportedOperationException( "Can't convert " + type + " from bytes" );
        }
    }
}
