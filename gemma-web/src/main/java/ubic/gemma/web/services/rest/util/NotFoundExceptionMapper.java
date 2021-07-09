package ubic.gemma.web.services.rest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    protected static final Log log = LogFactory.getLog( NotFoundExceptionMapper.class.getName() );

    protected static final String ERROR_MSG_UNMAPPED_PATH = "This URL is not mapped to any API call.";

    @Override
    public Response toResponse( NotFoundException e ) {
        WebService.log.warn( "Someone attempted a GET on an unmapped route.", e );
        return Response.status( Response.Status.NOT_FOUND )
                .entity( new WellComposedErrorBody( Response.Status.NOT_FOUND, NotFoundExceptionMapper.ERROR_MSG_UNMAPPED_PATH ) )
                .build();
    }
}
