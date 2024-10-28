package ubic.gemma.rest.providers;

import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.io.MexMatrixBundler;

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

import static ubic.gemma.rest.DatasetsWebService.APPLICATION_10X_MEX;
import static ubic.gemma.rest.DatasetsWebService.APPLICATION_10X_MEX_TYPE;

@Provider
@Produces(APPLICATION_10X_MEX)
@Singleton
public class MexBundlerProvider implements MessageBodyWriter<ExpressionDataFileService.LockedPath> {

    private final MexMatrixBundler bundler = new MexMatrixBundler();

    @Override
    public boolean isWriteable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return ExpressionDataFileService.LockedPath.class.isAssignableFrom( type )
                && APPLICATION_10X_MEX_TYPE.isCompatible( mediaType );
    }

    @Override
    public long getSize( ExpressionDataFileService.LockedPath lockedPath, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return -1;
    }

    @Override
    public void writeTo( ExpressionDataFileService.LockedPath lockedPath, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream ) throws IOException, WebApplicationException {
        try {
            bundler.bundle( lockedPath.getPath(), entityStream );
        } finally {
            lockedPath.close();
        }
    }
}
