package ubic.gemma.web.services.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.QueryException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.ServletUtils;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.services.rest.util.args.FilterArg;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link QueryException} into a proper error response.
 *
 * @author poirigui
 */
@Provider
public class QueryExceptionMapper implements ExceptionMapper<QueryException> {

    private static Log log = LogFactory.getLog( QueryExceptionMapper.class.getName() );

    @Context
    private HttpServletRequest request;

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( QueryException e ) {
        log.error( "Unknown query exception while serving request: " + ServletUtils.summarizeRequest( request ) + ".", e );
        WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                FilterArg.ERROR_MSG_MALFORMED_REQUEST );
        return Response.status( error.getStatus() ).entity( new ResponseErrorObject( error, OpenApiUtils.getOpenApi( servletConfig ) ) ).build();
    }
}
