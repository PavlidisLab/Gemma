package ubic.gemma.rest.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.*;
import org.assertj.core.internal.Maps;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.InputStream;
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
                formatEntity( actual ) );
    }

    private String formatEntity( Response response ) {
        if ( response.bufferEntity() ) {
            return response.readEntity( String.class );
        } else {
            return response.getEntity().toString();
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
        objects.assertEqual( info, actual.getMediaType(), mediaType );
        return myself;
    }

    /**
     * Asserts that the response has a media type compatible with the given media type.
     */
    public ResponseAssert hasMediaTypeCompatibleWith( MediaType mediaType ) {
        objects.assertEqual( info, actual.getMediaType().isCompatible( mediaType ), true );
        return myself;
    }

    public MapAssert<String, List<String>> headers() {
        return new MapAssert<>( actual.getStringHeaders() );
    }

    /**
     * Asserts that the response has the given header.
     */
    public ResponseAssert hasHeader( String name ) {
        maps.assertContainsKey( info, actual.getStringHeaders(), name );
        return myself;
    }

    /**
     * Asserts that the response has the given header with a particular value.
     * <p>
     * If the header is multivalued, asserts that at least one value is satisfied.
     */
    public ResponseAssert hasHeaderWithValue( String name, String value ) {
        maps.assertHasEntrySatisfying( info, actual.getStringHeaders(), name,
                new Condition<>( l -> l.contains( value ), "associated to %s", name ) );
        return myself;
    }

    public ResponseAssert hasHeaderSatisfying( String name, Consumer<List<String>> consumer ) {
        maps.assertHasEntrySatisfying( info, actual.getStringHeaders(), name, consumer );
        return myself;
    }

    /**
     * Asserts that the response does not have the given header.
     */
    public ResponseAssert doesNotHaveHeader( String name ) {
        maps.assertDoesNotContainKey( info, actual.getStringHeaders(), name );
        return myself;
    }

    /**
     * Asserts that the response does not have the given header with a particular value.
     * <p>
     * If the header is multivalued, asserts that no value is satisfied.
     */
    public ResponseAssert doesNotHaveHeaderWithValue( String name, String value ) {
        Map.Entry<String, String> entry = org.assertj.core.util.Maps.newHashMap( name, value ).entrySet().iterator().next();
        //noinspection unchecked
        maps.assertDoesNotContain( info, actual.getStringHeaders(), new Map.Entry[] { entry } );
        return myself;
    }

    /**
     * Asserts that the response has a 'Content-Length' header.
     */
    public ResponseAssert hasLength() {
        return hasHeader( "Content-Length" );
    }

    /**
     * Asserts that the response has a 'Content-Length' header with the given length.
     */
    public ResponseAssert hasLength( int length ) {
        objects.assertEqual( info, actual.getLength(), length );
        return myself;
    }

    /**
     * Asserts that the response has the given 'Content-Encoding' header.
     */
    public ResponseAssert hasEncoding( String encoding ) {
        return hasHeaderWithValue( "Content-Encoding", encoding );
    }

    public ResponseAssert doesNotHaveEncoding( String encoding ) {
        return doesNotHaveHeaderWithValue( "Content-Encoding", encoding );
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
}
