package ubic.gemma.rest.providers;

import ubic.gemma.core.analysis.service.ExpressionDataFileService.LockedPath;

import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * Write a {@link LockedPath} to a message body and ensure that the lock is released.
 * @author poirigui
 */
@Provider
@Produces({ "application/octet-stream", "*/*" })
@Singleton
public class LockedPathProvider implements MessageBodyWriter<LockedPath> {

    private final PathProvider pathProvider = new PathProvider();

    @Override
    public boolean isWriteable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return LockedPath.class.isAssignableFrom( type );
    }

    @Override
    public long getSize( LockedPath lockedPath, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return pathProvider.getSize( lockedPath.getPath(), type, genericType, annotations, mediaType );
    }

    @Override
    public void writeTo( LockedPath lockedPath, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream ) throws IOException, WebApplicationException {
        try {
            pathProvider.writeTo( lockedPath.getPath(), Path.class, genericType, annotations, mediaType, httpHeaders, entityStream );
        } finally {
            lockedPath.close();
        }
    }
}
