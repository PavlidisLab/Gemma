package ubic.gemma.rest.providers;

import org.glassfish.jersey.message.internal.FileProvider;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * Allow for reading and writing {@link Path} objects.
 * <p>
 * TODO: remove this once Jersey has been updated.
 * @author poirigui
 */
@Provider
@Produces({ "application/octet-stream", "*/*" })
@Consumes({ "application/octet-stream", "*/*" })
@Singleton
public class PathProvider implements MessageBodyReader<Path>, MessageBodyWriter<Path> {

    private final FileProvider fileProvider = new FileProvider();

    @Override
    public boolean isReadable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return Path.class == type;
    }

    @Override
    public Path readFrom( Class<Path> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream ) throws IOException, WebApplicationException {
        return fileProvider.readFrom( File.class, genericType, annotations, mediaType, httpHeaders, entityStream ).toPath();
    }

    @Override
    public boolean isWriteable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return Path.class.isAssignableFrom( type );
    }

    @Override
    public long getSize( Path path, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return fileProvider.getSize( path.toFile(), File.class, genericType, annotations, mediaType );
    }

    @Override
    public void writeTo( Path path, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream ) throws IOException, WebApplicationException {
        fileProvider.writeTo( path.toFile(), File.class, genericType, annotations, mediaType, httpHeaders, entityStream );
    }
}
