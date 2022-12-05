package ubic.gemma.web.services.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.common.auditAndSecurity.UserValueObject;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Hardcoded list of {@link ubic.gemma.model.common.description.ExternalDatabase} names to display on the root
     * endpoint.
     * TODO: use a {@link ubic.gemma.model.common.description.DatabaseType} to identify those.
     */
    private static final String[] EXTERNAL_DATABASE_NAMES = Settings.getStringArray( "gemma.externalDatabases.featured" );

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private UserManager userManager;

    /**
     * Returns an object with API information.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve an object with basic API information")
    public ResponseDataObject<ApiInfoValueObject> getApiInfo( // Params:
            // The servlet response, needed for response code setting.
            @Context final HttpServletRequest request,
            @Context final ServletConfig servletConfig ) {
        // collect various versioned entities to display on the main endpoint
        List<ExternalDatabaseValueObject> versioned = externalDatabaseService.findAllByNameIn( Arrays.asList( EXTERNAL_DATABASE_NAMES ) ).stream()
                .map( ExternalDatabaseValueObject::new )
                .collect( Collectors.toList() );
        URI apiDocsUrl = ServletUriComponentsBuilder.fromContextPath( request )
                .path( "/resources/restapidocs/" )
                .build()
                .toUri();
        return Responder.respond( new ApiInfoValueObject( MSG_WELCOME, OpenApiUtils.getOpenApi( servletConfig ), apiDocsUrl, versioned ) );
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

    @Value
    public static class ApiInfoValueObject {
        String welcome;
        String version;
        URI docs;
        List<ExternalDatabaseValueObject> externalDatabases;

        public ApiInfoValueObject( String msgWelcome, OpenAPI openApi, URI apiDocsUrl, List<ExternalDatabaseValueObject> externalDatabases ) {
            this.welcome = msgWelcome;
            if ( openApi.getInfo() != null ) {
                this.version = openApi.getInfo().getVersion();
            } else {
                this.version = null;
            }
            this.docs = apiDocsUrl;
            this.externalDatabases = externalDatabases;
        }
    }

}
