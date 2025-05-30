package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.util.Sendfile;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author poirigui
 * @see Sendfile
 */
@Provider
@CommonsLog
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
        if ( request != null && sendfileEnabled ) {
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
}
