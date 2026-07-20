package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseReadService;
import ubic.gemma.rest.util.BuildInfoValueObject;
import ubic.gemma.rest.util.ResponseDataObject;

import org.springframework.lang.Nullable;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
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
@Slf4j
public class RootWebService {

    private static final String MSG_WELCOME = "Welcome to Gemma RESTful API.";

    @Autowired
    private ExternalDatabaseReadService externalDatabaseService;

    @Autowired
    private UserManager userManager;

    @Autowired
    @Qualifier("openApi")
    private Future<OpenAPI> openApi;

    @Autowired
    private BuildInfo buildInfo;

    @Nullable
    @Autowired(required = false)
    private ServletContext servletContext;

    @Value("${gemma.externalDatabases.featured}")
    private String[] featuredExternalDatabases;

    /**
     * Returns an object with API information.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve an object with basic API information",
            description = "The payload contains a list of featured external databases that Gemma uses under the `externalDatabases` field. Those are mainly genomic references and sources of gene annotations.")
    public ResponseDataObject<ApiInfoValueObject> getApiInfo( @Context UriInfo uriInfo ) {
        // collect various versioned entities to display on the main endpoint
        List<ExternalDatabaseValueObject> versioned;
        if ( featuredExternalDatabases != null && featuredExternalDatabases.length > 0 ) {
            versioned = externalDatabaseService.findAllByNameIn( Arrays.asList( featuredExternalDatabases ) ).stream()
                    .map( ExternalDatabaseValueObject::new )
                    .collect( Collectors.toList() );
        } else {
            versioned = Collections.emptyList();
        }
        // API docs are hosted in a different servlet mapping, so we need to create an URL relative to the servlet context path
        URI apiDocsUrl = UriBuilder.fromPath( servletContext != null ? servletContext.getContextPath() : "" )
                .path( "/resources/restapidocs/" )
                .build();
        URI specUrl = uriInfo.getBaseUriBuilder()
                .scheme( null ).host( null ).port( -1 )
                .path( "/openapi.json" )
                .build();
        return respond( new ApiInfoValueObject( MSG_WELCOME, FutureUtils.get( openApi ), apiDocsUrl, specUrl, versioned, buildInfo ) );
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

    // (Curation-UI compatibility alias `GET /me` was removed — it conflicted with
    // AuthWebService.me() at Jersey resource-model validation. AuthWebService.me()
    // is now the canonical /me handler; this class still serves /users/me.)

    /**
     * Self-service password change for the authenticated user. Requires the current
     * password to be presented so a hijacked session or leaked token can't silently
     * rotate the credential. Routes through {@link UserManager#changePassword}, which
     * verifies the current password against the stored hash, enforces a minimum length,
     * and re-encodes the new password.
     */
    @PUT
    @Path("/users/me/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change the authenticated user's own password",
            description = "Requires the current password and a new password (minimum 8 characters). "
                    + "Returns 204 on success, 400 if the current password is wrong or the new password is too short.",
            security = {
                    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "basicAuth"),
                    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "cookieAuth")
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204",
                            description = "Password changed."),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                            description = "Missing fields, wrong current password, or new password too short.") })
    public Response changeMyPassword( ChangePasswordRequest req ) {
        if ( req == null || req.currentPassword == null || req.currentPassword.isEmpty()
                || req.newPassword == null || req.newPassword.isEmpty() ) {
            throw new BadRequestException( "currentPassword and newPassword are required" );
        }
        try {
            userManager.changePassword( req.currentPassword, req.newPassword );
        } catch ( org.springframework.security.authentication.BadCredentialsException e ) {
            // Don't distinguish "wrong current password" beyond a generic 400 message.
            throw new BadRequestException( "The current password is incorrect." );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( e.getMessage() );
        }
        return Response.noContent().build();
    }

    /**
     * Top-level alias for {@code GET /datasets/categories}: the curation-UI calls {@code GET /categories} for the
     * recently-used annotation-category picker. Implemented as a 302 redirect so query params (filter, limit,
     * etc.) pass through unchanged.
     */
    @GET
    @Path("/categories")
    @Operation(summary = "Retrieve usage statistics of categories among datasets (alias of /datasets/categories)", hidden = true,
            responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302",
                    description = "Redirection to /datasets/categories.") })
    public Response getCategoriesAlias( @Context UriInfo uriInfo ) {
        UriBuilder builder = uriInfo.getBaseUriBuilder()
                .scheme( null ).host( null ).port( -1 )
                .path( "/datasets/categories" );
        uriInfo.getQueryParameters().forEach( ( k, vs ) -> vs.forEach( v -> builder.queryParam( k, v ) ) );
        return Response.status( Response.Status.FOUND )
                .location( builder.build() )
                .build();
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
        List<String> authorities = resolveAuthorities( user );
        return new UserValueObject( user, group, authorities );
    }

    /**
     * Resolve the user's Spring Security authorities (GROUP_ADMIN, GROUP_USER, …) as
     * a sorted string list. The curation-UI gates admin surfaces on the presence of
     * {@code GROUP_ADMIN} here; without this field the SPA had no signal to tell
     * admin from non-admin and had to fall back to hide-when-anonymous gating.
     * Routed through {@link UserManager#loadUserByUsername} so the lookup works the
     * same on /users/me and on an admin-querying /users/{username} call — neither
     * relies on the looked-up user being the current authentication principal.
     */
    private List<String> resolveAuthorities( User user ) {
        UserDetails details = userManager.loadUserByUsername( user.getUserName() );
        return details.getAuthorities().stream()
                .map( GrantedAuthority::getAuthority )
                .sorted()
                .collect( Collectors.toList() );
    }

    @lombok.Value
    public static class ApiInfoValueObject {
        String welcome;
        String version;
        URI documentationUrl;
        URI specificationUrl;
        List<ExternalDatabaseValueObject> externalDatabases;
        BuildInfoValueObject buildInfo;

        public ApiInfoValueObject( String msgWelcome, OpenAPI openApi, URI apiDocsUrl, URI specUrl, List<ExternalDatabaseValueObject> externalDatabases, BuildInfo buildInfo ) {
            this.welcome = msgWelcome;
            if ( openApi.getInfo() != null ) {
                this.version = openApi.getInfo().getVersion();
            } else {
                this.version = null;
            }
            this.documentationUrl = apiDocsUrl;
            this.specificationUrl = specUrl;
            this.externalDatabases = externalDatabases;
            this.buildInfo = BuildInfoValueObject.from( buildInfo );
        }

        @Deprecated
        public URI getDocs() {
            return documentationUrl;
        }
    }

    /**
     * Request body for {@link #changeMyPassword}. Both fields are required.
     */
    public static class ChangePasswordRequest {
        /** The user's current password, re-verified before the change is applied. */
        public String currentPassword;
        /** The desired new password (minimum 8 characters). */
        public String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword( String currentPassword ) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword( String newPassword ) {
            this.newPassword = newPassword;
        }
    }

    /**
     * @author keshav
     */
    @lombok.Value
    public static class UserValueObject {
        String userName;
        String email;
        boolean enabled;
        @Nullable
        String group;
        /**
         * Spring Security authorities granted to this user (e.g. {@code GROUP_ADMIN},
         * {@code GROUP_USER}). Sorted alphabetically for stable wire output. The
         * curation-UI checks {@code authorities.includes("GROUP_ADMIN")} to decide
         * whether to render admin surfaces; prior to 2026-06-05 this was anonymous-only
         * because the user payload carried no role signal at all.
         */
        List<String> authorities;

        public UserValueObject( User user, @Nullable String group, List<String> authorities ) {
            userName = user.getUserName();
            email = user.getEmail();
            enabled = user.isEnabled();
            this.group = group;
            this.authorities = authorities;
        }
    }
}
