package ubic.gemma.web.services.rest.util;

import gemma.gsec.AuthorityConstants;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

/**
 * Base class for Jersey-based integration tests.
 *
 * @author poirigui
 */
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml",
        "classpath*:gemma/gsec/applicationContext-*.xml",
        "classpath:ubic/gemma/testDataSource.xml" })
public abstract class BaseJerseyIntegrationTest extends BaseJerseyTest {

    @Autowired
    @Qualifier("authenticationManager")
    private ProviderManager providerManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.runAsAdmin();
    }

    private void runAsAdmin() {
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) } ) );

        token.setAuthenticated( true );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication( token );
        SecurityContextHolder.setContext( securityContext );
    }
}
