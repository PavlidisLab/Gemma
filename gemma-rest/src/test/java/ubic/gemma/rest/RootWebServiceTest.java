package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.rest.util.Assertions;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

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

    @Value("${gemma.externalDatabases.featured}")
    private List<String> featuredExternalDatabases;

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
