package ubic.gemma.core.util.test;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * Base class for integration tests.
 * <p>
 * Tagged twice during the JUnit 5 migration: the JUnit 4 {@code @Category(IntegrationTest.class)}
 * keeps the Vintage engine selecting these classes via the legacy category interface, and the
 * JUnit 5 {@code @Tag("integration")} makes them selectable by the Jupiter engine and by tag-aware
 * Failsafe configuration. Subclasses do not need to repeat either annotation.
 * @author poirigui
 */
@Category(IntegrationTest.class)
@Tag("integration")
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" })
public abstract class BaseIntegrationTest extends BaseTest {

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
