package ubic.gemma.web.services.rest.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by tesarst on 17/05/17.
 * Class that translates a {@link GemmaApiException} to an API response.
 */
@Provider
public class GemmaApiExceptionMapper implements ExceptionMapper<GemmaApiException> {

    @Override
    public Response toResponse( final GemmaApiException exception ) {
        return Response.status( exception.getCode() ).entity( exception.getErrorObject() ).build();
    }
}
