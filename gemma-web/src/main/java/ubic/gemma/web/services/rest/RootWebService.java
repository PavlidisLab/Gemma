package ubic.gemma.web.services.rest;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.common.auditAndSecurity.UserValueObject;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

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
    private static final String ERROR_MSG_USER_INFO_ACCESS = "Inappropriate privileges. Only your user info is available.";

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
     * is executed before this code is called. This method then checks that the given username match the
     * user logged in the current session, and if so, creates a response with the logged in user information.
     *
     * @param uName the username
     */
    @GET
    @Path("users/{uname: [a-zA-Z0-9_]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @PreAuthorize("hasRole('GROUP_USER')")
    public ResponseDataObject loadUser( // Params:
            @PathParam("uname") String uName, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return this.checkUser( uName, sr );
    }

    private ResponseDataObject checkUser( String uName, HttpServletResponse sr ) {
        User user = userManager.getCurrentUser();

        // Check the logged in user is the one we are retrieving the info for
        if ( !user.getUserName().equals( uName ) ) {
            Response.Status code = Response.Status.FORBIDDEN;
            return Responder.code( code, new WellComposedErrorBody( code, ERROR_MSG_USER_INFO_ACCESS ), sr );
        }

        // Convert to a VO and check for admin
        UserValueObject uvo = new UserValueObject( user );
        Collection<String> groups = userManager.findGroupsForUser( user.getUserName() );
        for ( String g : groups ) {
            if ( g.equals( "Administrators" ) ) {
                uvo.setCurrentGroup( g );
                uvo.setInGroup( true );
            }
        }

        return Responder.autoCode( uvo, sr );
    }

    @SuppressWarnings("unused") // Getters used during RS serialization
    private static class ApiInfoValueObject {
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
