package ubic.gemma.web.services.rest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.JsonMappingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse( JsonMappingException exception ) {
        Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;

        LogFactory.getLog( this.getClass().getName() ).error( "Exception during JSON Mapping: " + exception );

        //FIXME remove before production - possibly we do not want to tell end-users the cause of the problem?
        WellComposedErrorBody errorBody = new WellComposedErrorBody( code, exception.getMessage() );
        return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
    }
}
