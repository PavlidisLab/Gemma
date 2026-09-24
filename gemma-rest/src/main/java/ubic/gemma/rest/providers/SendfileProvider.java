package ubic.gemma.rest.providers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.util.Sendfile;

import org.springframework.lang.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.zip.DeflaterOutputStream;

/**
 * @author poirigui
 * @see Sendfile
 */
@Provider
@Slf4j
@Component
@Produces({ "application/octet-stream", "*/*" })
public class SendfileProvider implements MessageBodyWriter<Sendfile> {

    private final PathProvider pathProvider = new PathProvider();

    @Value("${tomcat.sendfile.enabled}")
    private boolean sendfileEnabled;

    @Context
    @Nullable
    private HttpServletRequest request;

    @Override
    public boolean isWriteable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return Sendfile.class == type;
    }

    @Override
    public long getSize( Sendfile sendfile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return pathProvider.getSize( sendfile.getPath(), type, genericType, annotations, mediaType );
    }

    @Override
    public void writeTo( Sendfile sendfile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream ) throws IOException, WebApplicationException {
        if ( request != null && sendfileEnabled && !isEncoded( entityStream ) ) {
            if ( Boolean.TRUE.equals( request.getAttribute( "org.apache.tomcat.sendfile.support" ) ) ) {
                long size = getSize( sendfile, type, genericType, annotations, mediaType );
                request.setAttribute( "org.apache.tomcat.sendfile.filename", sendfile.getPath().toString() );
                request.setAttribute( "org.apache.tomcat.sendfile.start", 0L );
                request.setAttribute( "org.apache.tomcat.sendfile.end", size );
                httpHeaders.putSingle( "Content-Length", size );
                return;
            } else {
                log.warn( "Tomcat sendfile is not supported for this request. Falling back to stream download." );
            }
        }
        pathProvider.writeTo( sendfile.getPath(), type, genericType, annotations, mediaType, httpHeaders, entityStream );
    }

    /**
     * Whether a {@link jakarta.ws.rs.ext.WriterInterceptor} has wrapped the entity stream in a compressing stream,
     * in which case Tomcat sendfile must not be used.
     * <p>
     * Sendfile writes the file at the connector level and never touches {@code entityStream}, so anything an
     * encoder wrapped around that stream is bypassed: the response carries the encoder's {@code Content-Encoding}
     * header over unencoded bytes, and clients that honour the header cannot read it at all. That combination hit
     * {@code /resultSets/{resultSet}} in its TSV representation.
     * <p>
     * Falling back to a regular streamed download keeps the response correct at the cost of sendfile's zero-copy.
     * The right fix at the call site is to serve a pre-encoded file and declare
     * {@code @GZIP(alreadyCompressed = true)}, which appends the header after the encoder so the stream is never
     * wrapped and this guard never fires — every sendfile endpoint in the API does it that way.
     * <p>
     * Detecting the wrapper rather than the header is deliberate: by the time a {@code MessageBodyWriter} runs,
     * every interceptor has already set its headers, so a {@code Content-Encoding} check would also match the
     * correct {@code alreadyCompressed} endpoints and silently drop them off the sendfile path. Covers Jersey's
     * gzip and deflate encoders, both of which wrap in a {@link DeflaterOutputStream}.
     */
    private static boolean isEncoded( OutputStream entityStream ) {
        if ( entityStream instanceof DeflaterOutputStream ) {
            log.warn( "The entity stream has been wrapped for content encoding; Tomcat sendfile would bypass it "
                    + "and emit unencoded bytes under an encoding header. Falling back to a streamed download." );
            return true;
        }
        return false;
    }
}
