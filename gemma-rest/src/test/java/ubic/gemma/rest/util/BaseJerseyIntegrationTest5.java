package ubic.gemma.rest.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.TestAuthenticationUtils;

/**
 * JUnit 5 (Jupiter) base class for Jersey integration tests, tagged
 * {@code @Tag("integration")} for surefire/failsafe filtering.
 *
 * @see BaseJerseyIntegrationTest
 * @author poirigui
 */
@Tag("integration")
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" })
public abstract class BaseJerseyIntegrationTest5 extends BaseJerseyTest5 {

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

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
