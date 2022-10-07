package ubic.gemma.web.services.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.QueryException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.ServletUtils;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

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
public class QueryExceptionMapper extends AbstractExceptionMapper<QueryException> {

    @Override
    protected Response.Status getStatus( QueryException exception ) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( QueryException exception ) {
        return new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                "Entity does not contain the given property, or the provided value can not be converted to the property type." );
    }

    @Override
    protected boolean logException() {
        return true;
    }
}
