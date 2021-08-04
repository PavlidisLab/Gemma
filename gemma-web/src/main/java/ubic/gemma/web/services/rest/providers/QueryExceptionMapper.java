package ubic.gemma.web.services.rest.providers;

import org.hibernate.QueryException;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.services.rest.util.args.FilterArg;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link QueryException} into proper
 * @author poirigui
 */
@Provider
public class QueryExceptionMapper implements ExceptionMapper<QueryException> {

    @Override
    public Response toResponse( QueryException e ) {
        WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                FilterArg.ERROR_MSG_MALFORMED_REQUEST );
        return Response.status( error.getStatus() )
                .entity( new ResponseErrorObject( error ) )
                .build();
    }
}
