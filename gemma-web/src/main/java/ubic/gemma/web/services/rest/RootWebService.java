package ubic.gemma.web.services.rest;

import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Handles calls to the root API url.
 *
 * @author tesarst
 */
@Path("/")
public class RootWebService extends WebService {

    private static final String MSG_WELCOME = "Welcome to Gemma RESTful API.";
    private static final String APIDOCS_URL = "http://www.chibi.ubc.ca/Gemma/resources/restapidocs/";

    /**
     * Required by spring
     */
    public RootWebService() {
    }

    /**
     * Returns an object with API information.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject all( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code200( new ApiInfoValueObject(), sr );
    }

    @SuppressWarnings("unused") // Getters used during RS serialization
    private class ApiInfoValueObject {
        private String welcome = MSG_WELCOME;
        private String version = WebService.API_VERSION;
        private String docs = APIDOCS_URL;

        public String getWelcome() {
            return welcome;
        }

        public String getVersion() {
            return version;
        }

        public String getDocs() {
            return docs;
        }
    }

}
