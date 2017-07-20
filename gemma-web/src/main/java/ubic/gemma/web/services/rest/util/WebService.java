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

    protected static final String ERR_MSG_UNMAPPED_PATH = "This URL is not mapped to any API call.";
    protected static final Log log = LogFactory.getLog( WebService.class.getName() );
    static final String API_VERSION = "2.0";
    private static String ERROR_MSG_ENTITY_NOT_FOUND = "Entity with the given identifier does not exist or is not accessible.";

    @GET
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyGet( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a GET on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    @POST
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyPost( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a POST on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    @DELETE
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyDelete( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a DELETE on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    @PUT
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyPut( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        log.warn( "Someone attempted a PUT on " + uriInfo.getAbsolutePath() );
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    protected <T extends MutableArg> ResponseDataObject autoCodeResponse( T mutableArg, Object response,
            HttpServletResponse sr ) {
        if ( response == null ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    ERROR_MSG_ENTITY_NOT_FOUND );
            WellComposedErrorBody.addExceptionFields( error, new EntityNotFoundException( mutableArg.getNullCause() ) );
            response = error;
        }
        return Responder.autoCode( response, sr );
    }

}
