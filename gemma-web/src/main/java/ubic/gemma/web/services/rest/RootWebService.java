package ubic.gemma.web.services.rest;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Handles calls to the root API url and user info api
 *
 * @author tesarst
 */
@Service
@Path("/")
public class RootWebService extends WebService {

    private static final String MSG_WELCOME = "Welcome to Gemma RESTful API.";
    private static final String APIDOCS_URL = Settings.getBaseUrl() + "resources/restapidocs/";

    private UserManager userManager;

    /**
     * Required by spring
     */
    @SuppressWarnings("unused")
    public RootWebService() {
    }

    @Autowired
    public RootWebService( UserManager userManager ) {
        this.userManager = userManager;
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

    /**
     * Retrieves user information. This method is pre-authorized, which means that a session login (via basic http auth)
     * is executed before this code is called. This method then checks that the given username and password match the
     * user logged in the current session, and if so, creates a response with the logged in user information.
     *
     * @param uName the username
     * @param pHash a sha256 hash of the users password
     */
    @GET
    @Path("users/{uname: [a-zA-Z0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @PreAuthorize( "hasRole('GROUP_USER')" )
    public ResponseDataObject datasetPlatforms( // Params:
            @PathParam("uname") String uName, // Required
            @QueryParam("phash") String pHash, // Required, default 1
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( this.checkUser(uName, pHash), sr );
    }

    private User checkUser( String uName, String pHash ) {
        User user = userManager.getCurrentUser();
        if( !user.getUserName().equals( uName ) || !DigestUtils.sha256Hex( user.getPassword() ).equals( pHash ) ) return null;
        return user;
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
