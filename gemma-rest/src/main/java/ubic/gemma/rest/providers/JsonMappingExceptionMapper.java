package ubic.gemma.rest.providers;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.rest.util.OpenApiUtils;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;
import ubic.gemma.rest.util.ServletUtils;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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

    private static final Log log = LogFactory.getLog( JsonMappingExceptionMapper.class.getName() );

    @Context
    private HttpServletRequest request;

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( JsonMappingException exception ) {
        log.error( "Exception during JSON mapping for request: " + ServletUtils.summarizeRequest( request ) + ".", exception );
        Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
        WellComposedErrorBody errorBody = new WellComposedErrorBody( code, exception.getMessage() );
        return Response.status( code )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .entity( new ResponseErrorObject( errorBody, OpenApiUtils.getOpenApi( servletConfig ) ) ).build();
    }
}
