package ubic.gemma.persistence.hibernate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
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

    private final LobHandler lobHandler = new DefaultLobHandler();

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
        InputStream gzippedStream = lobHandler.getBlobAsBinaryStream( rs, names[0] );
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
    public void nullSafeSet( PreparedStatement st, Object value, int index, SessionImplementor session ) throws HibernateException, SQLException {
        Assert.notNull( delimiter, "The 'delimiter' parameter must be set." );
        byte[] blob;
        if ( value != null ) {
            //noinspection unchecked
            List<String> s = ( List<String> ) value;
            Assert.isTrue( s.stream().noneMatch( k -> k.contains( delimiter ) ),
                    String.format( "The list of strings may not contain the delimiter %s.", delimiter ) );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try ( OutputStream out = new GZIPOutputStream( baos ) ) {
                IOUtils.write( String.join( delimiter, s ), out, StandardCharsets.UTF_8 );
            } catch ( IOException e ) {
                throw new HibernateException( e );
            }
            blob = baos.toByteArray();
        } else {
            blob = null;
        }
        lobHandler.getLobCreator().setBlobAsBytes( st, index, blob );
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException {
        return value != null ? new ArrayList<>( ( List<String> ) value ) : null;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble( Object value ) throws HibernateException {
        return value != null ? String.join( delimiter, ( List<String> ) value ) : null;
    }

    @Override
    public Object assemble( Serializable cached, Object owner ) throws HibernateException {
        return cached != null ? Arrays.asList( StringUtils.split( ( String ) cached, delimiter ) ) : null;
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException {
        return deepCopy( original );
    }

    @Override
    public void setParameterValues( Properties parameters ) {
        this.delimiter = ( String ) requireNonNull( parameters.get( "delimiter" ),
                "A non-null value must be used as delimiter." );
    }
}