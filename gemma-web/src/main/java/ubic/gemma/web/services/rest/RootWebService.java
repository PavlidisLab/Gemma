package ubic.gemma.web.services.rest;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
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
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
    public ResponseDataObject<ApiInfoValueObject> getApiInfo( // Params:
            // The servlet response, needed for response code setting.
            @Context final ServletConfig servletConfig ) {
        return Responder.respond( new ApiInfoValueObject( MSG_WELCOME, OpenApiUtils.getOpenApi( servletConfig ), APIDOCS_URL ) );
    }

    /**
     * Retrieve user information.
     *
     * This method only works for authenticated users (via basic HTTP auth or their JSESSIONID cookie as specified by
     * the {@link SecurityScheme} annotation on this class. If the current authenticated user is an administrator, any
     * user can be retrieved with this endpoint, otherwise only the current user is accessible.
     *
     * @param username the username
     */
    @GET
    @Path("/users/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("(isAuthenticated() && principal.username == #username) || hasRole('GROUP_ADMIN')")
    @Operation(summary = "Retrieve the user information associated to the authenticated session", hidden = true)
    public ResponseDataObject<UserValueObject> getUser( // Params:
            @PathParam("username") String username // Required
    ) {
        User user = userManager.findByUserName( username );

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
