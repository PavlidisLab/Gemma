package ubic.gemma.core.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
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
        Assert.isTrue( endpoint.startsWith( "/" ) );
        URLConnection connection = null;
        try {
            int status;
            connection = new URL( hostUrl + "/rest/v2" + endpoint ).openConnection();
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
                } else if ( status >= 300 && status < 400 ) {
                    // redirection
                    return new RedirectionImpl( httpConnection.getHeaderField( "Location" ) );
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

    @Data
    @EqualsAndHashCode(callSuper = false)
    private static class RedirectionImpl extends ResponseImpl implements Redirection {
        private final String location;
    }

    private static class EmptyResponseImpl extends ResponseImpl implements EmptyResponse {

    }
}