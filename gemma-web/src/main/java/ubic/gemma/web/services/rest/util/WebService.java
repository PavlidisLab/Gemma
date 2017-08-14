package ubic.gemma.web.services.rest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.web.services.rest.util.args.MutableArg;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created by tesarst on 18/05/17.
 * A class containing common functionality for all other API services. E.g. a fallback mapping that presents a 404
 * response code with appropriate payload.
 *
 * @author tesarst
 */
public abstract class WebService {

    protected static final String ERROR_MSG_UNMAPPED_PATH = "This URL is not mapped to any API call.";
    protected static final Log log = LogFactory.getLog( WebService.class.getName() );
    static final String API_VERSION = "2.0";

    @GET
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyGet( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a GET on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERROR_MSG_UNMAPPED_PATH, sr );
    }

    @POST
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyPost( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a POST on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERROR_MSG_UNMAPPED_PATH, sr );
    }

    @DELETE
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyDelete( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a DELETE on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERROR_MSG_UNMAPPED_PATH, sr );
    }

    @PUT
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyPut( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a PUT on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERROR_MSG_UNMAPPED_PATH, sr );
    }

}
