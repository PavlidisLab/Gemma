package ubic.gemma.web.services.rest;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.common.auditAndSecurity.UserValueObject;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;

import javax.servlet.ServletConfig;
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
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic", description = "Authenticate with your Gemma username and password")
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "JSESSIONID", description = "Authenticate with your current Gemma session.")
@CommonsLog
public class RootWebService {

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
    @Operation(summary = "Retrieve an object with basic API information")
    public ResponseDataObject<ApiInfoValueObject> all( // Params:
            @Context final HttpServletResponse sr, // The servlet response, needed for response code setting.
            @Context final ServletConfig servletConfig ) {
        return Responder.respond( new ApiInfoValueObject( MSG_WELCOME, OpenApiUtils.getOpenApi( servletConfig ), APIDOCS_URL ) );
    }

    /**
     * Retrieves user information. This method is pre-authorized, which means that a session login (via basic http auth)
     * is executed before this code is called. This method then checks that the given username match the
     * user logged in the current session, and if so, creates a response with the logged in user information.
     *
     * @param uName the username
     */
    @GET
    @Path("/users/{uname}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasRole('GROUP_USER')")
    @Operation(summary = "Retrieve the user information associated to the authenticated session")
    public ResponseDataObject<UserValueObject> loadUser( // Params:
            @PathParam("uname") String uName, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return this.checkUser( uName, sr );
    }

    private ResponseDataObject<UserValueObject> checkUser( String uName, HttpServletResponse sr ) throws AccessDeniedException {
        User user = userManager.getCurrentUser();

        // Check the logged in user is the one we are retrieving the info for
        if ( !user.getUserName().equals( uName ) ) {
            Response.Status code = Response.Status.FORBIDDEN;
            throw new AccessDeniedException( ERROR_MSG_USER_INFO_ACCESS );
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

        return Responder.respond( uvo );
    }

    @SuppressWarnings("unused") // Getters used during RS serialization
    @Getter
    public static class ApiInfoValueObject {
        private final String welcome;
        private final String version;
        private final String docs;

        public ApiInfoValueObject( String msgWelcome, OpenAPI openApi, String apidocsUrl ) {
            this.welcome = msgWelcome;
            if ( openApi.getInfo() != null ) {
                this.version = openApi.getInfo().getVersion();
            } else {
                this.version = null;
            }
            this.docs = apidocsUrl;
        }
    }

}
