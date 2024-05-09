package ubic.gemma.rest.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.*;
import org.assertj.core.internal.Maps;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Assertions for jax-rs {@link Response}.
 * @author poirigui
 */
public class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {

    private final Maps maps = Maps.instance();

    public ResponseAssert( Response actual ) {
        super( actual, ResponseAssert.class );
        info.description( "\nHTTP/1.1 %d %s\n%s\n\n%s\n",
                actual.getStatus(), actual.getStatusInfo().getReasonPhrase(),
                actual.getStringHeaders().entrySet().stream()
                        .sorted( Map.Entry.comparingByKey() )
                        .map( e -> e.getKey() + ": " + String.join( ", ", e.getValue() ) )
                        .collect( Collectors.joining( "\n" ) ),
                formatEntity( actual.getEntity() ) );
    }

    private String formatEntity( Object entity ) {
        if ( entity instanceof ByteArrayInputStream ) {
            try {
                return IOUtils.toString( ( InputStream ) entity, StandardCharsets.UTF_8 );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            } finally {
                ( ( ByteArrayInputStream ) entity ).reset();
            }
        } else {
            return entity.toString();
        }
    }

    /**
     * Asserts that the response has the given status code and reason phrase.
     */
    public ResponseAssert hasStatus( Response.Status status ) {
        objects.assertEqual( info, actual.getStatus(), status.getStatusCode() );
        objects.assertEqual( info, actual.getStatusInfo().getReasonPhrase(), status.getReasonPhrase() );
        return myself;
    }

    /**
     * Asserts that that response status is within the given status family.
     */
    public ResponseAssert hasStatusFamily( Response.Status.Family family ) {
        objects.assertEqual( info, actual.getStatusInfo().getFamily(), family );
        return myself;
    }

    /**
     * Asserts that the response has the given media type.
     */
    public ResponseAssert hasMediaType( MediaType mediaType ) {
        objects.assertEqual( info, mediaType, mediaType );
        return myself;
    }

    /**
     * Asserts that the response has a media type compatible with the given media type.
     */
    public ResponseAssert hasMediaTypeCompatibleWith( MediaType mediaType ) {
        objects.assertEqual( info, mediaType.isCompatible( mediaType ), true );
        return myself;
    }

    public MapAssert<String, List<String>> headers() {
        return new MapAssert<>( actual.getStringHeaders() );
    }


    /**
     * Asserts that the response has the given header.
     * <p>
     * If the header is multivalued, asserts that at least one value is satisfied.
     */
    public ResponseAssert hasHeader( String name, String value ) {
        maps.assertHasEntrySatisfying( info, actual.getStringHeaders(), name,
                new Condition<>( l -> l.contains( value ), "associated to %s", name ) );
        return myself;
    }

    public ResponseAssert hasHeaderSatisfying( String name, Consumer<List<String>> consumer ) {
        maps.assertHasEntrySatisfying( info, actual.getStringHeaders(), name, consumer );
        return myself;
    }

    /**
     * Asserts that the response has the given 'Content-Encoding' header.
     */
    public ResponseAssert hasEncoding( String encoding ) {
        return hasHeader( "Content-Encoding", encoding );
    }

    public ResponseAssert hasLanguage( Locale locale ) {
        objects.assertEqual( info, actual.getLanguage(), locale );
        return myself;
    }

    public MapAssert<String, NewCookie> cookies() {
        return new MapAssert<>( actual.getCookies() );
    }

    public ObjectAssert<?> entity() {
        return new ObjectAssert<>( actual.readEntity( Object.class ) );
    }

    public <T> ObjectAssert<T> entityAs( Class<T> clazz ) {
        try {
            return new ObjectAssert<>( actual.readEntity( clazz ) );
        } catch ( ProcessingException e ) {
            throw failure( "Failed to read entity as %s: %s", clazz, ExceptionUtils.getRootCauseMessage( e ) );
        }
    }

    public StringAssert entityAsString() {
        return new StringAssert( actual.readEntity( String.class ) );
    }

    public InputStreamAssert entityAsStream() {
        return new InputStreamAssert( actual.readEntity( InputStream.class ) );
    }

    public ResponseAssert hasLength( int length ) {
        objects.assertEqual( info, actual.getLength(), length );
        return myself;
    }
}
