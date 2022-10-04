package ubic.gemma.web.services.rest.providers;

import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static ubic.gemma.web.services.rest.util.ExceptionMapperUtils.acceptsJson;

/**
 * Map {@link WebApplicationException} so that it always expose a {@link ResponseErrorObject} entity.
 *
 * @author poirigui
 */
@Provider
public class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {

    @Override
    protected Response.Status getStatus( WebApplicationException exception ) {
        return Response.Status.fromStatusCode( exception.getResponse().getStatus() );
    }
}
