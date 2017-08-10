package ubic.gemma.web.services.rest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    public Response toResponse( final Throwable throwable ) {

        Log log = LogFactory.getLog( this.getClass().getName() );
        log.error( "Exception caught during API call: " + throwable.getMessage() );

        //FIXME put the if back before release
        //if ( log.isDebugEnabled() ) {
        throwable.printStackTrace();
        //}

        if ( throwable instanceof GemmaApiException ) {
            GemmaApiException exception = ( GemmaApiException ) throwable;
            return Response.status( exception.getCode() ).entity( exception.getErrorObject() ).build();
        } else {
            Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
            WellComposedErrorBody errorBody = new WellComposedErrorBody( code, throwable.getMessage() );
            return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
        }
    }
}

