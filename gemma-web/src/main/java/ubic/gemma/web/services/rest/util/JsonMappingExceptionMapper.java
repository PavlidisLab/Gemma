package ubic.gemma.web.services.rest.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Mapper that creates a well composed error body response out of a JsonMappingException.
 *
 * @author tesarst
 */
@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse( JsonMappingException exception ) {
        Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;

        LogFactory.getLog( this.getClass().getName() ).error( "Exception during JSON Mapping: " + exception );
        WellComposedErrorBody errorBody = new WellComposedErrorBody( code, exception.getMessage() );
        return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
    }
}
