package ubic.gemma.web.services.rest.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by tesarst on 17/05/17.
 * Class that translates a {@link GemmaApiException} to an API response.
 */
@Provider
public class GemmaApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse( final Throwable throwable) {
        if(throwable instanceof GemmaApiException) {
            GemmaApiException exception = ( GemmaApiException ) throwable;
            return Response.status( exception.getCode() ).entity( exception.getErrorObject() ).build();
        }else{
            Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
            //FIXME remove before production - possibly we do not want to tell end-users the cause of the problem?
            WellComposedErrorBody errorBody = new WellComposedErrorBody( code.getStatusCode()+"", throwable.getMessage() );
            return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
        }
    }
}
