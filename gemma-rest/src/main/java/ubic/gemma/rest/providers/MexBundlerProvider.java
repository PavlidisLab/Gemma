package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.datastructure.matrix.io.MexMatrixBundler;

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

import static ubic.gemma.rest.DatasetsWebService.APPLICATION_10X_MEX;
import static ubic.gemma.rest.DatasetsWebService.APPLICATION_10X_MEX_TYPE;

@Provider
@Produces(APPLICATION_10X_MEX)
@CommonsLog
public class MexBundlerProvider implements MessageBodyWriter<Path> {

    private final MexMatrixBundler bundler = new MexMatrixBundler();

    @Override
    public boolean isWriteable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return Path.class.isAssignableFrom( type )
                && APPLICATION_10X_MEX_TYPE.isCompatible( mediaType );
    }

    @Override
    public long getSize( Path path, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        try {
            return bundler.calculateSize( path );
        } catch ( IOException e ) {
            log.warn( "Failed to calculate size of MEX bundle at " + path + ".", e );
            return -1;
        }
    }

    @Override
    public void writeTo( Path path, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream ) throws IOException, WebApplicationException {
        bundler.bundle( path, entityStream );
    }
}
