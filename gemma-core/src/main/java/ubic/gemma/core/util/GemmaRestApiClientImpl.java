package ubic.gemma.core.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@CommonsLog
public class GemmaRestApiClientImpl implements GemmaRestApiClient {

    private final String hostUrl;

    private final ObjectMapper objectMapper;

    private String username, password;

    public GemmaRestApiClientImpl( String hostUrl ) {
        this( hostUrl, new ObjectMapper().setDateFormat( new StdDateFormat() ) );
    }

    public GemmaRestApiClientImpl( String hostUrl, ObjectMapper objectMapper ) {
        Assert.isTrue( StringUtils.isNotBlank( hostUrl ), "The host url cannot be blank." );
        Assert.isTrue( !hostUrl.endsWith( "/" ), "THe host URL must not end with a '/' character." );
        this.hostUrl = hostUrl;
        this.objectMapper = objectMapper;
    }

    /**
     * Set the authentication credentials used by this client.
     */
    @Override
    public void setAuthentication( String username, String password ) {
        Assert.isTrue( StringUtils.isNotBlank( username ), "A non-blank username must be supplied." );
        Assert.isTrue( StringUtils.isNotBlank( password ), "A non-blank password must be supplied." );
        this.username = username;
        this.password = password;
    }

    @Override
    public void clearAuthentication() {
        this.username = null;
        this.password = null;
    }

    @Override
    public Response perform( String endpoint ) throws IOException {
        return performInternal( endpoint, null );
    }

    @Override
    public Response perform( String endpoint, MultiValueMap<String, Object> params ) throws IOException {
        return performInternal( endpoint, params );
    }

    public Response perform( String endpoint, String firstParamName, Object firstParamValue, Object... otherParams ) throws IOException {
        Assert.isTrue( StringUtils.isNotBlank( firstParamName ), "Parameter names must not be blank." );
        Assert.isTrue( otherParams.length % 2 == 0, "The number of parameters must be even." );
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add( firstParamName, firstParamValue );
        for ( int i = 0; i < otherParams.length; i += 2 ) {
            Assert.isInstanceOf( String.class, otherParams[i], "Parameter names must be strings." );
            Assert.isTrue( StringUtils.isNotBlank( ( String ) otherParams[i] ), "Parameter names must not be blank." );
            params.add( ( String ) otherParams[i], otherParams[i + 1] );
        }
        return performInternal( endpoint, params );
    }

    private Response performInternal( String endpoint, @Nullable MultiValueMap<String, Object> params ) throws IOException {
        Assert.isTrue( endpoint.startsWith( "/" ), "Endpoint must start with a '/' character." );
        URLConnection connection = null;
        try {
            int status;
            URL url = new URL( hostUrl + "/rest/v2" + endpoint + ( params != null ? "?" + encodeQueryParams( params ) : "" ) );
            connection = url.openConnection();
            connection.setRequestProperty( "Accept", "application/json" );
            connection.setRequestProperty( "Accept-Encoding", "gzip" );
            if ( username != null ) {
                connection.setRequestProperty( "Authorization", "Basic " + Base64.getEncoder().encodeToString( ( username + ":" + password ).getBytes() ) );
            }
            if ( connection instanceof HttpURLConnection ) {
                HttpURLConnection httpConnection = ( HttpURLConnection ) connection;
                status = httpConnection.getResponseCode();
                if ( status >= 100 && status < 200 ) {
                    // informational
                    return new EmptyResponseImpl();
                } else if ( status >= 200 && status < 300 ) {
                    if ( status == HttpURLConnection.HTTP_NO_CONTENT ) {
                        return new EmptyResponseImpl();
                    }
                    return readJsonResponseFromStream( httpConnection.getInputStream(), connection.getContentEncoding(), DataResponseImpl.class );
                } else if ( status >= 400 && status < 600 ) {
                    return readJsonResponseFromStream( httpConnection.getErrorStream(), connection.getContentEncoding(), ErrorResponseImpl.class );
                } else {
                    throw new IOException( "Unexpected status code for HTTP response: " + status + " " + httpConnection.getResponseMessage() );
                }
            }
            // without a status code, we have to let Jackson guess...
            return readJsonResponseFromStream( connection.getInputStream(), connection.getContentEncoding(), ResponseImpl.class );
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
    }

    private String encodeQueryParams( MultiValueMap<String, Object> params ) {
        return params.entrySet().stream()
                .flatMap( e -> e.getValue().stream().map( v -> encode( e.getKey() ) + "=" + encode( stringify( v ) ) ) )
                .collect( Collectors.joining( "&" ) );
    }

    /**
     * Stringify an object into something understood by Gemma REST.
     * TODO: handle lists as comma-delimited strings
     */
    private String stringify( Object o ) {
        return String.valueOf( o );
    }

    private String encode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    private <T> T readJsonResponseFromStream( InputStream stream, @Nullable String contentEncoding, Class<T> clazz ) throws IOException {
        if ( "gzip".equalsIgnoreCase( contentEncoding ) ) {
            stream = new GZIPInputStream( stream );
        } else if ( contentEncoding != null ) {
            throw new IOException( "Unsupported content encoding: " + contentEncoding );
        }
        return objectMapper.readValue( stream, clazz );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    // FIXME: this is not working from the CLI, but it's working in tests, this is necessary if the URLConnection is not
    //        an HttpURLConnection
    // @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    // @JsonSubTypes({ @JsonSubTypes.Type(DataResponseImpl.class), @JsonSubTypes.Type(ErrorResponseImpl.class) })
    private abstract static class ResponseImpl implements Response {

    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    private static class DataResponseImpl extends ResponseImpl implements DataResponse {
        private Object data;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    private static class ErrorResponseImpl extends ResponseImpl implements ErrorResponse {
        private String apiVersion;
        private ErrorImpl error;

        @Data
        private static class ErrorImpl implements Error {
            private int code;
            private String message;
        }
    }

    private static class EmptyResponseImpl extends ResponseImpl implements EmptyResponse {

    }
}