package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.authentication.UserDetailsImpl;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.rest.util.Assertions;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

public class RootWebServiceTest extends BaseJerseyIntegrationTest5 {

    @Autowired
    @Qualifier("openApi")
    private Future<OpenAPI> openApi;

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${gemma.externalDatabases.featured}")
    private List<String> featuredExternalDatabases;

    /**
     * Provision an enabled user with a known password using the real encoder, so the
     * self-service change-password path has a genuine credential to verify against.
     * Runs as admin (set by the base @BeforeEach).
     */
    private void provisionUser( String username, String password ) {
        userManager.createUser( username, username + "@example.org", password );
        // createUser leaves the account disabled (email-confirm flow) with the password
        // already encoded; re-enable it, preserving the encoded hash (updateUser stores verbatim).
        User created = userManager.findByUserName( username );
        userManager.updateUser( new UserDetailsImpl( created.getPassword(), username, true, null,
                created.getEmail(), null, null ) );
    }

    /**
     * End-to-end confirmation that an authenticated user can rotate their own password:
     * PUT /users/me/password with the correct current password re-encodes the credential.
     * This is the primary "we have a valid, secure self-service path" regression guard.
     */
    @Test
    public void testChangeMyPasswordEndToEnd() {
        provisionUser( "rootws-pw-user", "OldPassw0rd" );

        testAuthenticationUtils.runAsUser( "rootws-pw-user", false );
        RootWebService.ChangePasswordRequest req = new RootWebService.ChangePasswordRequest();
        req.currentPassword = "OldPassw0rd";
        req.newPassword = "NewPassw0rd1";
        Assertions.assertThat( target( "/users/me/password" ).request().put( Entity.json( req ) ) )
                .hasStatus( Response.Status.NO_CONTENT );

        // The stored hash now matches the new password and no longer the old one.
        testAuthenticationUtils.runAsAdmin();
        User reloaded = userManager.findByUserName( "rootws-pw-user" );
        assertThat( passwordEncoder.matches( "NewPassw0rd1", reloaded.getPassword() ) ).isTrue();
        assertThat( passwordEncoder.matches( "OldPassw0rd", reloaded.getPassword() ) ).isFalse();
    }

    /**
     * A wrong current password must be rejected with 400 — a hijacked session can't
     * rotate the credential without proving knowledge of the existing one.
     */
    @Test
    public void testChangeMyPasswordWrongCurrentIs400() {
        provisionUser( "rootws-pw-user2", "OldPassw0rd" );

        testAuthenticationUtils.runAsUser( "rootws-pw-user2", false );
        RootWebService.ChangePasswordRequest req = new RootWebService.ChangePasswordRequest();
        req.currentPassword = "TotallyWrong";
        req.newPassword = "NewPassw0rd1";
        Assertions.assertThat( target( "/users/me/password" ).request().put( Entity.json( req ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );

        // password unchanged: the old one still verifies.
        testAuthenticationUtils.runAsAdmin();
        User reloaded = userManager.findByUserName( "rootws-pw-user2" );
        assertThat( passwordEncoder.matches( "OldPassw0rd", reloaded.getPassword() ) ).isTrue();
    }

    /**
     * A new password below the minimum length is rejected with 400.
     */
    @Test
    public void testChangeMyPasswordTooShortIs400() {
        provisionUser( "rootws-pw-user3", "OldPassw0rd" );

        testAuthenticationUtils.runAsUser( "rootws-pw-user3", false );
        RootWebService.ChangePasswordRequest req = new RootWebService.ChangePasswordRequest();
        req.currentPassword = "OldPassw0rd";
        req.newPassword = "short";
        Assertions.assertThat( target( "/users/me/password" ).request().put( Entity.json( req ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void testChangeMyPasswordMissingFieldsIs400() {
        // authenticated as admin from @BeforeEach; empty body → 400 before any service call
        Assertions.assertThat( target( "/users/me/password" ).request()
                .put( Entity.json( new RootWebService.ChangePasswordRequest() ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void test() {
        String expectedVersion = FutureUtils.get( openApi ).getInfo().getVersion();
        assertThat( expectedVersion ).isNotBlank();
        assertThat( featuredExternalDatabases ).isNotEmpty();
        Assertions.assertThat( target( "/" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data" )
                .hasFieldOrPropertyWithValue( "version", expectedVersion )
                .hasFieldOrPropertyWithValue( "docs", "/resources/restapidocs/" )
                .extracting( "externalDatabases", list( Object.class ) )
                .extracting( "name" )
                .containsExactlyElementsOf( featuredExternalDatabases );
    }

    /**
     * /users/me must carry the user's Spring Security authorities so the curation-UI
     * can gate admin surfaces on {@code GROUP_ADMIN} membership. Prior to 2026-06-05
     * the payload had no role signal and the SPA fell back to anonymous-only gating.
     * Base test class sets admin in @BeforeEach.
     */
    @Test
    public void testGetMyselfAsAdminExposesAdminAuthority() {
        Assertions.assertThat( target( "/users/me" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data" )
                .extracting( "authorities", list( String.class ) )
                .contains( AuthorityConstants.ADMIN_GROUP_AUTHORITY );
    }

    @Test
    public void testGetMyselfAsRegularUserExposesUserAuthorityOnly() {
        try {
            testAuthenticationUtils.runAsUser( "rootws-test-user", true );
            Assertions.assertThat( target( "/users/me" ).request().get() )
                    .hasStatus( Response.Status.OK )
                    .entity()
                    .extracting( "data" )
                    .extracting( "authorities", list( String.class ) )
                    .contains( AuthorityConstants.USER_GROUP_AUTHORITY )
                    .doesNotContain( AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        } finally {
            testAuthenticationUtils.runAsAdmin();
        }
    }
}
