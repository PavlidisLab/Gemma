package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.rest.util.OpenApiUtils;
import ubic.gemma.rest.util.ResponseDataObject;

import javax.annotation.Nullable;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.respond;

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
    @Operation(summary = "Retrieve an object with basic API information",
            description = "The payload contains a list of featured external databases that Gemma uses under the `externalDatabases` field. Those are mainly genomic references and sources of gene annotations.")
    public ResponseDataObject<ApiInfoValueObject> getApiInfo( // Params:
            // The servlet response, needed for response code setting.
            @Context final HttpServletRequest request,
            @Context final ServletConfig servletConfig ) {
        // collect various versioned entities to display on the main endpoint
        List<ExternalDatabaseValueObject> versioned;
        if ( EXTERNAL_DATABASE_NAMES != null && EXTERNAL_DATABASE_NAMES.length > 0 ) {
            versioned = externalDatabaseService.findAllByNameIn( Arrays.asList( EXTERNAL_DATABASE_NAMES ) ).stream()
                    .map( ExternalDatabaseValueObject::new )
                    .collect( Collectors.toList() );
        } else {
            versioned = Collections.emptyList();
        }
        URI apiDocsUrl = ServletUriComponentsBuilder.fromContextPath( request )
                .scheme( null ).host( null ).port( -1 )
                .path( "/resources/restapidocs/" )
                .build()
                .toUri();
        return respond( new ApiInfoValueObject( MSG_WELCOME, OpenApiUtils.getOpenApi( servletConfig ), apiDocsUrl, versioned ) );
    }

    /**
     * Retrieve user information for the current user.
     */
    @GET
    @Path("/users/me")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve the user information associated to the authenticated session", hidden = true)
    public ResponseDataObject<UserValueObject> getMyself() {
        return respond( getUserVo( userManager.getCurrentUser() ) );
    }

    /**
     * Retrieve user information.
     * <p>
     * This method only works for authenticated users (via basic HTTP auth or their JSESSIONID cookie as specified by
     * the {@link SecurityScheme} annotation on this class. If the current authenticated user is an administrator, any
     * user can be retrieved with this endpoint, otherwise only the current user is accessible.
     *
     * @param username the username
     */
    @GET
    @Path("/users/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("(isAuthenticated() && principal.username == #username) || hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve the user information associated to the given username", hidden = true)
    public ResponseDataObject<UserValueObject> getUser( // Params:
            @PathParam("username") String username // Required
    ) {
        User user = userManager.findByUserName( username );
        if ( user == null ) {
            throw new NotFoundException( String.format( "No user with username %s.", username ) );
        }
        return respond( getUserVo( user ) );
    }

    private UserValueObject getUserVo( User user ) {
        // Convert to a VO and check for admin
        String group = userManager.findGroupsForUser( user.getUserName() ).stream().findFirst().orElse( null );
        return new UserValueObject( user, group );
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

    /**
     * @author keshav
     */
    @Value
    public static class UserValueObject {
        String userName;
        String email;
        boolean enabled;
        @Nullable
        String group;

        public UserValueObject( User user, @Nullable String group ) {
            userName = user.getUserName();
            email = user.getEmail();
            enabled = user.getEnabled();
            this.group = group;
        }
    }
}
