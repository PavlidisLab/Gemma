package ubic.gemma.web.services.rest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A class containing common functionality for all other API services. E.g. a fallback mapping that presents a 404
 * response code with appropriate payload.
 *
 * @author tesarst
 */
public abstract class WebService {

    protected static final Log log = LogFactory.getLog( WebService.class.getName() );
    protected static final String API_VERSION = "2.3.4";

    protected void checkReqArg( Object arg, String name ) {
        if ( arg == null || arg.toString().isEmpty() ) {
            Response.Status code = Response.Status.BAD_REQUEST;
            WellComposedErrorBody errorBody = new WellComposedErrorBody( code,
                    String.format( "Value for required parameter '%s' not found.", name ) );
            throw new GemmaApiException( errorBody );
        }
    }
}
