package ubic.gemma.persistence.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.springframework.util.Assert;
import ubic.gemma.persistence.util.ByteArrayUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Properties;

/**
 * Represents a vector of scalars stored as a byte array in a single column.
 * <p>
 * The following types are supported for the {@code arrayType} parameter:
 * <ul>
 *     <li>{@code boolean}</li>
 *     <li>{@code char}</li>
 *     <li>{@code int}</li>
 *     <li>{@code long}</li>
 *     <li>{@code float}</li>
 *     <li>{@code double}</li>
 *     <li>{@link String}, either zero-terminated or tab-delimited</li>
 * </ul>
 * Other types supported by {@link ByteArrayUtils} can be added if necessary.
 *
 * @author poirigui
 * @see ByteArrayUtils
 */
public class ByteArrayType implements UserType, ParameterizedType {

    private enum ByteArrayTypes {
        BOOLEAN( boolean[].class ),
        CHAR( char[].class ),
        INT( int[].class ),
        LONG( long[].class ),
        FLOAT( float[].class ),
        DOUBLE( double[].class ),
        STRING( String[].class ),
        TABBED_STRING( String[].class );

        private final Class<?> arrayClass;

        ByteArrayTypes( Class<?> arrayClass ) {
            this.arrayClass = arrayClass;
        }
    }

    /**
     * Type of the array elements.
     */
    private ByteArrayTypes arrayType;

    /**
     * Charset to use for encoding and decoding strings.
     * <p>
     * This is only allowed for string types.
     * <p>
     * Defaults to {@link StandardCharsets#UTF_8}.
     */
    private Charset charset;

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.BLOB };
    }

    @Override
    public Class<?> returnedClass() {
        return arrayType.arrayClass;
    }

    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        switch ( arrayType ) {
            case BOOLEAN:
                return Arrays.equals( ( boolean[] ) x, ( boolean[] ) y );
            case CHAR:
                return Arrays.equals( ( char[] ) x, ( char[] ) y );
            case INT:
                return Arrays.equals( ( int[] ) x, ( int[] ) y );
            case LONG:
                return Arrays.equals( ( long[] ) x, ( long[] ) y );
            case FLOAT:
                return Arrays.equals( ( float[] ) x, ( float[] ) y );
            case DOUBLE:
                return Arrays.equals( ( double[] ) x, ( double[] ) y );
            case STRING:
            case TABBED_STRING:
                return Arrays.equals( ( String[] ) x, ( String[] ) y );
            default:
                throw unsupportedArrayType( arrayType );
        }
    }

    @Override
    public int hashCode( Object x ) throws HibernateException {
        switch ( arrayType ) {
            case BOOLEAN:
                return Arrays.hashCode( ( boolean[] ) x );
            case CHAR:
                return Arrays.hashCode( ( char[] ) x );
            case INT:
                return Arrays.hashCode( ( int[] ) x );
            case LONG:
                return Arrays.hashCode( ( long[] ) x );
            case FLOAT:
                return Arrays.hashCode( ( float[] ) x );
            case DOUBLE:
                return Arrays.hashCode( ( double[] ) x );
            case STRING:
            case TABBED_STRING:
                return Arrays.hashCode( ( String[] ) x );
            default:
                throw unsupportedArrayType( arrayType );
        }
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SessionImplementor session, Object owner ) throws HibernateException, SQLException {
        Blob blob = rs.getBlob( names[0] );
        if ( blob != null ) {
            byte[] data = blob.getBytes( 1, ( int ) blob.length() );
            switch ( arrayType ) {
                case BOOLEAN:
                    return ByteArrayUtils.byteArrayToBooleans( data );
                case CHAR:
                    return ByteArrayUtils.byteArrayToChars( data );
                case INT:
                    return ByteArrayUtils.byteArrayToInts( data );
                case LONG:
                    return ByteArrayUtils.byteArrayToLongs( data );
                case FLOAT:
                    return ByteArrayUtils.byteArrayToFloats( data );
                case DOUBLE:
                    return ByteArrayUtils.byteArrayToDoubles( data );
                case STRING:
                    return ByteArrayUtils.byteArrayToStrings( data, charset );
                case TABBED_STRING:
                    return ByteArrayUtils.byteArrayToTabbedStrings( data, charset );
                default:
                    throw unsupportedArrayType( arrayType );
            }
        } else {
            return null;
        }
    }

    @Override
    public void nullSafeSet( PreparedStatement st, @Nullable Object value, int index, SessionImplementor session ) throws HibernateException, SQLException {
        if ( value != null ) {
            byte[] blob;
            switch ( arrayType ) {
                case BOOLEAN:
                    blob = ByteArrayUtils.booleanArrayToBytes( ( boolean[] ) value );
                    break;
                case CHAR:
                    blob = ByteArrayUtils.charArrayToBytes( ( char[] ) value );
                    break;
                case INT:
                    blob = ByteArrayUtils.intArrayToBytes( ( int[] ) value );
                    break;
                case LONG:
                    blob = ByteArrayUtils.longArrayToBytes( ( long[] ) value );
                    break;
                case FLOAT:
                    blob = ByteArrayUtils.floatArrayToBytes( ( float[] ) value );
                    break;
                case DOUBLE:
                    blob = ByteArrayUtils.doubleArrayToBytes( ( double[] ) value );
                    break;
                case STRING:
                    blob = ByteArrayUtils.stringsToByteArray( ( String[] ) value, charset );
                    break;
                case TABBED_STRING:
                    blob = ByteArrayUtils.stringsToTabbedBytes( ( String[] ) value, charset );
                    break;
                default:
                    throw unsupportedArrayType( arrayType );
            }
            st.setBytes( index, blob );
        } else {
            st.setBytes( index, null );
        }
    }

    @Override
    public Object deepCopy( @Nullable Object value ) throws HibernateException {
        if ( value == null ) {
            return null;
        }
        switch ( arrayType ) {
            case BOOLEAN:
                return ( ( boolean[] ) value ).clone();
            case CHAR:
                return ( ( char[] ) value ).clone();
            case INT:
                return ( ( int[] ) value ).clone();
            case LONG:
                return ( ( long[] ) value ).clone();
            case FLOAT:
                return ( ( float[] ) value ).clone();
            case DOUBLE:
                return ( ( double[] ) value ).clone();
            case STRING:
            case TABBED_STRING:
                return ( ( String[] ) value ).clone();
            default:
                throw unsupportedArrayType( arrayType );
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble( Object value ) throws HibernateException {
        return ( Serializable ) deepCopy( value );
    }

    @Override
    public Object assemble( Serializable cached, Object owner ) throws HibernateException {
        return deepCopy( cached );
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException {
        return deepCopy( original );
    }

    @Override
    public void setParameterValues( @Nullable Properties parameters ) {
        Assert.isTrue( parameters != null && parameters.containsKey( "arrayType" ),
                "There must be an 'arrayType' parameter in the type declaration." );
        arrayType = ByteArrayTypes.valueOf( parameters.getProperty( "arrayType" ).toUpperCase() );
        if ( parameters.containsKey( "charset" ) ) {
            Assert.isTrue( arrayType == ByteArrayTypes.STRING || arrayType == ByteArrayTypes.TABBED_STRING,
                    "The 'charset' parameter is only valid for string types." );
            charset = Charset.forName( parameters.getProperty( "charset" ) );
        } else {
            charset = StandardCharsets.UTF_8;
        }
    }

    private HibernateException unsupportedArrayType( ByteArrayTypes type ) {
        return new HibernateException( String.format( "Unsupported array type: %s.", type ) );
    }
}
