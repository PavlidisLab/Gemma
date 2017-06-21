package ubic.gemma.web.services.rest;

import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Created by tesarst on 17/05/17.
 * Handles calls to the root API url.
 */
@Path("/")
public class DefaultWebService extends WebService {

    /**
     * Required by spring
     */
    public DefaultWebService() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject all( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( ERR_MSG_UNMAPPED_PATH, sr );
    }

}
