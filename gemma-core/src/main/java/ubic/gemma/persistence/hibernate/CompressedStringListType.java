package ubic.gemma.persistence.hibernate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.stream.Streams;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.springframework.util.Assert;
import ubic.gemma.core.util.concurrent.ThreadUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Type that transparently stores a {@link List} of {@link String} as gzip-compressed blob.
 * @author poirigui
 */
public class CompressedStringListType implements UserType<List<String>>, ParameterizedType {

    /**
     * Delimiter to use for separating strings in the list.
     * <p>
     * It is not allowed to have this delimiter in any of the strings.
     */
    private String delimiter;

    /**
     * Charset to use for encoding and decoding strings.
     * <p>
     * Defaults to {@link StandardCharsets#UTF_8}.
     */
    private Charset charset;

    @Override
    public int getSqlType() {
        return Types.BLOB;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<String>> returnedClass() {
        return ( Class<List<String>> ) ( Class<?> ) List.class;
    }

    @Override
    public boolean equals( List<String> x, List<String> y ) throws HibernateException {
        return Objects.equals( x, y );
    }

    @Override
    public int hashCode( List<String> x ) throws HibernateException {
        return Objects.hashCode( x );
    }

    @Override
    public List<String> nullSafeGet( ResultSet rs, int position, SharedSessionContractImplementor session, Object owner ) throws HibernateException, SQLException {
        Assert.notNull( delimiter, "The 'delimiter' parameter must be set." );
        InputStream gzippedStream = rs.getBinaryStream( position );
        if ( gzippedStream != null ) {
            return decompress( gzippedStream );
        } else {
            return null;
        }
    }

    @Override
    public void nullSafeSet( PreparedStatement st, @Nullable List<String> value, int index, SharedSessionContractImplementor session ) throws HibernateException, SQLException {
        Assert.notNull( delimiter, "The 'delimiter' parameter must be set." );
        if ( value != null ) {
            st.setBlob( index, compress( value ) );
        } else {
            st.setBlob( index, ( InputStream ) null );
        }
    }

    @Override
    public List<String> deepCopy( @Nullable List<String> value ) throws HibernateException {
        return value != null ? new ArrayList<>( value ) : null;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble( @Nullable List<String> value ) throws HibernateException {
        return ( Serializable ) deepCopy( value );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> assemble( @Nullable Serializable cached, Object owner ) throws HibernateException {
        return deepCopy( ( List<String> ) cached );
    }

    @Override
    public List<String> replace( List<String> original, List<String> target, Object owner ) throws HibernateException {
        return deepCopy( original );
    }

    @Override
    public void setParameterValues( @Nullable Properties parameters ) {
        String m = "A non-null value must be used as delimiter.";
        this.delimiter = ( String ) requireNonNull( requireNonNull( parameters, m ).get( "delimiter" ), m );
        if ( parameters.containsKey( "charset" ) ) {
            this.charset = Charset.forName( parameters.getProperty( "charset" ) );
        } else {
            this.charset = StandardCharsets.UTF_8;
        }
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
    public InputStream compress( List<String> s ) {
        Assert.isTrue( s.stream().noneMatch( k -> k.contains( delimiter ) ),
                String.format( "The list of strings may not contain the delimiter %s.", delimiter ) );
        PipedInputStream is;
        PipedOutputStream out;
        try {
            out = new PipedOutputStream();
            is = new PipedInputStream( out );
        } catch ( IOException e ) {
            throw new HibernateException( e );
        }
        ThreadUtils.newThread( () -> {
            try ( Writer w = new OutputStreamWriter( new GZIPOutputStream( out ), charset ) ) {
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

    public List<String> decompress( InputStream gzippedStream ) {
        try ( InputStream in = new GZIPInputStream( gzippedStream ) ) {
            return Arrays.asList( StringUtils.splitByWholeSeparatorPreserveAllTokens( IOUtils.toString( in, charset ), delimiter ) );
        } catch ( IOException e ) {
            throw new HibernateException( e );
        }
    }

    public Stream<String> decompressToStream( InputStream gzippedStream ) {
        // Use decompress() then stream — same memory footprint for the only known caller
        // (small cell-id lists ~100s of items), and avoids two subtle issues that broke
        // SingleCellExpressionExperimentServiceTest.testGetSingleCellDimensionWithoutCellIds:
        //   1. Apache commons-lang3 Streams.of(Scanner) was producing a single-element stream
        //      (the whole compressed payload) on commons-lang3 3.18.0 — not splitting on the
        //      Scanner's configured delimiter.
        //   2. The delimiter parsed from the hbm.xml <param> is whatever Hibernate 6 hands us
        //      (a literal backslash-n string per the existing FIXME in SingleCellDimension.hbm.xml).
        //      Pattern.quote round-trip via Scanner has been historically fragile here.
        // decompress() reuses String.splitByWholeSeparatorPreserveAllTokens which is unambiguous.
        return decompress( gzippedStream ).stream();
    }
}
