package ubic.gemma.web.services.rest;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.User;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.common.auditAndSecurity.UserValueObject;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;

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

    public static final String API_VERSION = "2.3.4";
    private static final String MSG_WELCOME = "Welcome to Gemma RESTful API.";
    private static final String APIDOCS_URL = Settings.getBaseUrl() + "resources/restapidocs/";
    private static final String ERROR_MSG_USER_INFO_ACCESS = "Inappropriate privileges. Only your user info is available.";

    private UserManager userManager;

    @Autowired
    private OpenAPI openAPI;

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
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        log.debug( openAPI );
        return Responder.respond( new ApiInfoValueObject() );
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
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
    public class ApiInfoValueObject {
        private final String welcome = MSG_WELCOME;
        private final String version = openAPI.getInfo().getVersion();
        private final String docs = APIDOCS_URL;

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

    /**
     * Obtain the {@link OpenAPI} definition for this API.
     */
    @Bean
    public OpenAPI openAPI() {
        try {
            return new JaxrsOpenApiContextBuilder().buildContext( true ).read();
        } catch ( OpenApiConfigurationException e ) {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

}
