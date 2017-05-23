package ubic.gemma.web.services.rest.util;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Created by tesarst on 18/05/17.
 * A class containing common functionality for all other API services. E.g. a fallback mapping that presents a 404
 * response code with appropriate payload.
 */
public abstract class AbstractWebService {

    protected static final String ERR_MSG_UNMAPPED_PATH = "This URL is not mapped to any API call.";

    /* ********************************
     * API Methods
     * ********************************/

    @GET
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyGet( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    @POST
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyPost( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    @DELETE
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyDelete( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

    @PUT
    @Path("/{default: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject anyPut( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

}
