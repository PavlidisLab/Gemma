package ubic.gemma.persistence.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Properties;

/**
 * Represents a vector of scalars stored as a byte array in a single column.
 * <p>
 * The following types are supported for the {@code arrayType} parameter:
 * <ul>
 *     <li>{@code int}</li>
 *     <li>{@code double}</li>
 * </ul>
 * Other types supported by {@link ByteArrayUtils} can be added if necessary.
 * @author poirigui
 * @see ByteArrayUtils
 */
public class ByteArrayType implements UserType, ParameterizedType {

    private enum ByteArrayTypes {
        INT( int[].class ),
        DOUBLE( double[].class );

        private final Class<?> arrayClass;

        ByteArrayTypes( Class<?> arrayClass ) {
            this.arrayClass = arrayClass;
        }
    }

    private final LobHandler lobHandler = new DefaultLobHandler();

    private ByteArrayTypes arrayType;

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
            case INT:
                return Arrays.equals( ( int[] ) x, ( int[] ) y );
            case DOUBLE:
                return Arrays.equals( ( double[] ) x, ( double[] ) y );
            default:
                throw unsupportedArrayType( arrayType );
        }
    }

    @Override
    public int hashCode( Object x ) throws HibernateException {
        switch ( arrayType ) {
            case INT:
                return Arrays.hashCode( ( int[] ) x );
            case DOUBLE:
                return Arrays.hashCode( ( double[] ) x );
            default:
                throw unsupportedArrayType( arrayType );
        }
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SessionImplementor session, Object owner ) throws HibernateException, SQLException {
        byte[] data = lobHandler.getBlobAsBytes( rs, names[0] );
        if ( data != null ) {
            switch ( arrayType ) {
                case INT:
                    return ByteArrayUtils.byteArrayToInts( data );
                case DOUBLE:
                    return ByteArrayUtils.byteArrayToDoubles( data );
                default:
                    throw unsupportedArrayType( arrayType );
            }
        } else {
            return null;
        }
    }

    @Override
    public void nullSafeSet( PreparedStatement st, Object value, int index, SessionImplementor session ) throws HibernateException, SQLException {
        byte[] blob;
        if ( value != null ) {
            switch ( arrayType ) {
                case INT:
                    blob = ByteArrayUtils.intArrayToBytes( ( int[] ) value );
                    break;
                case DOUBLE:
                    blob = ByteArrayUtils.doubleArrayToBytes( ( double[] ) value );
                    break;
                default:
                    throw unsupportedArrayType( arrayType );
            }
        } else {
            blob = null;
        }
        lobHandler.getLobCreator().setBlobAsBytes( st, index, blob );
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException {
        if ( value == null ) {
            return null;
        }
        switch ( arrayType ) {
            case INT:
                return ( ( int[] ) value ).clone();
            case DOUBLE:
                return ( ( double[] ) value ).clone();
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
    public void setParameterValues( Properties parameters ) {
        Assert.isTrue( parameters != null && parameters.containsKey( "arrayType" ),
                "There must be an 'arrayType' parameter in the type declaration." );
        arrayType = ByteArrayTypes.valueOf( parameters.getProperty( "arrayType" ).toUpperCase() );
    }

    private HibernateException unsupportedArrayType( ByteArrayTypes type ) {
        return new HibernateException( String.format( "Unsupported array type: %s.", type ) );
    }
}
