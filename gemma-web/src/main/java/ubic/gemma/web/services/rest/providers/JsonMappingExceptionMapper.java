package ubic.gemma.web.services.rest.providers;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

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

    private static Log log = LogFactory.getLog( JsonMappingExceptionMapper.class.getName() );

    @Override
    public Response toResponse( JsonMappingException exception ) {
        log.error( "Exception during JSON mapping: ", exception );
        Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
        WellComposedErrorBody errorBody = new WellComposedErrorBody( code, exception.getMessage() );
        return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
    }
}
