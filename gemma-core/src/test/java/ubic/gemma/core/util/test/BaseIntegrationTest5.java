package ubic.gemma.core.util.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

/**
 * JUnit 5 (Jupiter) base class for integration tests, tagged
 * {@code @Tag("integration")} for surefire/failsafe filtering.
 * <p>
 * Spring's {@link SecurityContextHolder} is cleared after every test so
 * authentication state doesn't leak across runs.
 */
@Tag("integration")
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" })
public abstract class BaseIntegrationTest5 extends BaseTest5 {

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /**
     * Setup the authentication for the test.
     * <p>
     * The default is to grant an administrator authority to the current user.
     */
    @BeforeEach
    public final void setUpAuthentication() {
        testAuthenticationUtils.runAsAdmin();
    }

    /**
     * Clear the {@link SecurityContextHolder} so that subsequent tests don't inherit authentication.
     */
    @AfterEach
    public final void tearDownSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
