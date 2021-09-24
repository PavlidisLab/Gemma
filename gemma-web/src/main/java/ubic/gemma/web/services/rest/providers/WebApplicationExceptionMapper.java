package ubic.gemma.web.services.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link WebApplicationException} so that it always expose a {@link ResponseErrorObject} entity.
 *
 * @author poirigui
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static Log log = LogFactory.getLog( WebApplicationExceptionMapper.class.getName() );

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( WebApplicationException e ) {
        log.error( "Unknown error while serving request: ", e );
        return Response.fromResponse( e.getResponse() ).entity( new ResponseErrorObject(
                new WellComposedErrorBody( Response.Status.fromStatusCode( e.getResponse().getStatus() ),
                        e.getMessage() ), OpenApiUtils.getOpenApi( servletConfig ) ) ).build();
    }
}
