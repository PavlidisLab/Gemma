package ubic.gemma.core.util.test;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.jdbc.JdbcTestUtils;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * Base class for integration tests.
 * @author poirigui
 */
@ActiveProfiles(EnvironmentProfiles.TEST)
@Category(IntegrationTest.class)
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" })
public abstract class BaseIntegrationTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /**
     * Setup the authentication for the test.
     * <p>
     * The default is to grant an administrator authority to the current user.
     */
    @Before
    public final void setUpAuthentication() {
        testAuthenticationUtils.runAsAdmin();
    }

    /**
     * Clear the {@link SecurityContextHolder} so that subsequent tests don't inherit authentication.
     */
    @After
    public final void tearDownSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
