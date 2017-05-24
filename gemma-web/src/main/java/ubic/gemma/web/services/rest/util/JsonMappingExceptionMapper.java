package ubic.gemma.web.services.rest.util;

import org.codehaus.jackson.map.JsonMappingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse(JsonMappingException exception) {
        Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
        //FIXME remove before production - possibly we do not want to tell end-users the cause of the problem?
        WellComposedErrorBody errorBody = new WellComposedErrorBody( code.getStatusCode() + "",
                exception.getMessage() );
        return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
    }
}
