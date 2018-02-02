package ubic.gemma.web.services.rest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Mapper that creates a well composed error body response out of any Throwable.
 *
 * @author tesarst
 */
@Provider
public class GemmaApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse( final Throwable throwable ) {

        Log log = LogFactory.getLog( this.getClass().getName() );
        log.error( "Exception caught during API call: " + throwable.getMessage() );

//        if ( log.isDebugEnabled() ) {
            throwable.printStackTrace();
//        }

        if ( throwable instanceof GemmaApiException ) {
            GemmaApiException exception = ( GemmaApiException ) throwable;
            return Response.status( exception.getCode() ).entity( exception.getErrorObject() ).build();
        } else if ( throwable instanceof WebApplicationException ) {
            WebApplicationException internalException = ( WebApplicationException ) throwable;
            return internalException.getResponse();
        } else {
            Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
            if ( throwable instanceof AccessDeniedException ) {
                code = Response.Status.FORBIDDEN;
            }
            WellComposedErrorBody errorBody = new WellComposedErrorBody( code, throwable.getMessage() );
            return Response.status( code ).entity( new ResponseErrorObject( errorBody ) ).build();
        }
    }
}

