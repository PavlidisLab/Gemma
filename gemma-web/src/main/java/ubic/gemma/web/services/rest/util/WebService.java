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

    protected static final String ERROR_MSG_UNMAPPED_PATH = "This URL is not mapped to any API call.";
    protected static final Log log = LogFactory.getLog( WebService.class.getName() );
    protected static final String API_VERSION = "2.2.1";

    /**
     * Fallback for unmapped GET calls
     */
    @GET
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("permitAll")
    public ResponseDataObject anyGet( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        WebService.log.warn( "Someone attempted a GET on " + uriInfo.getAbsolutePath() );
        return Responder.code404( WebService.ERROR_MSG_UNMAPPED_PATH, sr );
    }

    /**
     * Fallback for unmapped POST calls
     */
    @POST
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseDataObject anyPost( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        WebService.log.warn( "Someone attempted a POST on " + uriInfo.getAbsolutePath() );
        return Responder.code404( WebService.ERROR_MSG_UNMAPPED_PATH, sr );
    }

    /**
     * Fallback for unmapped DELETE calls
     */
    @DELETE
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseDataObject anyDelete( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        WebService.log.warn( "Someone attempted a DELETE on " + uriInfo.getAbsolutePath() );
        return Responder.code404( WebService.ERROR_MSG_UNMAPPED_PATH, sr );
    }

    /**
     * Fallback for unmapped PUT calls
     */
    @PUT
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseDataObject anyPut( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context UriInfo uriInfo // The information about the URI that was requested
    ) {
        WebService.log.warn( "Someone attempted a PUT on " + uriInfo.getAbsolutePath() );
        return Responder.code404( WebService.ERROR_MSG_UNMAPPED_PATH, sr );
    }

    protected void checkReqArg( Object arg, String name ) {
        if ( arg == null || arg.toString().isEmpty() ) {
            Response.Status code = Response.Status.BAD_REQUEST;
            WellComposedErrorBody errorBody = new WellComposedErrorBody( code,
                    String.format( "Value for required parameter '%s' not found.", name ) );
            throw new GemmaApiException( errorBody );
        }
    }
}
