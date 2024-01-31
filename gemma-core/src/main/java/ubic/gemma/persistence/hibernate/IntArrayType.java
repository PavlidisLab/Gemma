package ubic.gemma.persistence.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import ubic.basecode.io.ByteArrayConverter;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Represents a vector of integers stored in a single column.
 * @author poirigui
 * @see ByteArrayConverter#byteArrayToInts(byte[])
 * @see ByteArrayConverter#intArrayToBytes(int[])
 */
public class IntArrayType implements UserType {

    private static final ByteArrayConverter converter = new ByteArrayConverter();

    private final LobHandler lobHandler = new DefaultLobHandler();

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.BLOB };
    }

    @Override
    public Class<?> returnedClass() {
        return int[].class;
    }

    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        return Arrays.equals( ( int[] ) x, ( int[] ) y );
    }

    @Override
    public int hashCode( Object x ) throws HibernateException {
        return Arrays.hashCode( ( int[] ) x );
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SessionImplementor session, Object owner ) throws HibernateException, SQLException {
        byte[] data = lobHandler.getBlobAsBytes( rs, 0 );
        if ( data != null ) {
            return converter.byteArrayToInts( data );
        } else {
            return null;
        }
    }

    @Override
    public void nullSafeSet( PreparedStatement st, Object value, int index, SessionImplementor session ) throws HibernateException, SQLException {
        byte[] blob;
        if ( value != null ) {
            blob = converter.intArrayToBytes( ( int[] ) value );
        } else {
            blob = null;
        }
        lobHandler.getLobCreator().setBlobAsBytes( st, index, blob );
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException {
        return ( ( int[] ) value ).clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble( Object value ) throws HibernateException {
        return ( int[] ) value;
    }

    @Override
    public Object assemble( Serializable cached, Object owner ) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException {
        return ( ( int[] ) original ).clone();
    }
}
