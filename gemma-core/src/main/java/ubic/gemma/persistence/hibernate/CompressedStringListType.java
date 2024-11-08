package ubic.gemma.persistence.hibernate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Type that transparently stores a {@link List} of {@link String} as gzip-compressed blob.
 * <p>
 * String are assumed to be encoded according to UTF-8.
 * @author poirigui
 */
public class CompressedStringListType implements UserType, ParameterizedType {

    private String delimiter = null;

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.BLOB };
    }

    @Override
    public Class<?> returnedClass() {
        return List.class;
    }

    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        return Objects.equals( x, y );
    }

    @Override
    public int hashCode( Object x ) throws HibernateException {
        return Objects.hashCode( x );
    }

    @Override
    public List<String> nullSafeGet( ResultSet rs, String[] names, SessionImplementor session, Object owner ) throws HibernateException, SQLException {
        Assert.notNull( delimiter, "The 'delimiter' parameter must be set." );
        InputStream gzippedStream = rs.getBinaryStream( names[0] );
        if ( gzippedStream != null ) {
            try ( InputStream is = new GZIPInputStream( gzippedStream ) ) {
                return Arrays.asList( StringUtils.split( IOUtils.toString( is, StandardCharsets.UTF_8 ), delimiter ) );
            } catch ( IOException e ) {
                throw new HibernateException( e );
            }
        } else {
            return null;
        }
    }

    @Override
    public void nullSafeSet( PreparedStatement st, @Nullable Object value, int index, SessionImplementor session ) throws HibernateException, SQLException {
        Assert.notNull( delimiter, "The 'delimiter' parameter must be set." );
        if ( value != null ) {
            //noinspection unchecked
            List<String> s = ( List<String> ) value;
            Assert.isTrue( s.stream().noneMatch( k -> k.contains( delimiter ) ),
                    String.format( "The list of strings may not contain the delimiter %s.", delimiter ) );
            st.setBlob( index, compress( s ) );
        } else {
            st.setBlob( index, ( InputStream ) null );
        }
    }

    @Override
    public Object deepCopy( @Nullable Object value ) throws HibernateException {
        //noinspection unchecked
        return value != null ? new ArrayList<>( ( List<String> ) value ) : null;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble( @Nullable Object value ) throws HibernateException {
        //noinspection unchecked
        return value != null ? String.join( delimiter, ( List<String> ) value ) : null;
    }

    @Override
    public Object assemble( @Nullable Serializable cached, Object owner ) throws HibernateException {
        return cached != null ? Arrays.asList( StringUtils.split( ( String ) cached, delimiter ) ) : null;
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException {
        return deepCopy( original );
    }

    @Override
    public void setParameterValues( @Nullable Properties parameters ) {
        String m = "A non-null value must be used as delimiter.";
        this.delimiter = ( String ) requireNonNull( requireNonNull( parameters, m ).get( "delimiter" ), m );
    }

    /**
     * Compress the given list of strings into a gzip-compressed input stream.
     * <p>
     * Because Java lacks a gzip-compressing input stream implementation, we create a pipe and write the compressed
     * output on one end and retrieve the it as an input stream on the other end.
     * <p>
     * I'm (poirigui) not too concerned by the thread creation since those are limited by the number of database
     * connection.
     * FIXME: replace this by a compressing input stream
     */
    private InputStream compress( List<String> s ) {
        PipedInputStream is;
        PipedOutputStream out;
        try {
            out = new PipedOutputStream();
            is = new PipedInputStream( out );
        } catch ( IOException e ) {
            throw new HibernateException( e );
        }
        new Thread( () -> {
            try ( Writer w = new OutputStreamWriter( new GZIPOutputStream( out ) ) ) {
                boolean first = true;
                for ( String s1 : s ) {
                    if ( !first ) {
                        w.write( delimiter );
                    }
                    w.write( s1 );
                    first = false;
                }
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } ).start();
        return is;
    }
}
