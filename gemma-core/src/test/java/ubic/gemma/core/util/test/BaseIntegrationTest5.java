package ubic.gemma.core.util.test;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseIntegrationTest}.
 * <p>
 * Carries both the JUnit 4 {@code @Category(IntegrationTest.class)} and the
 * JUnit 5 {@code @Tag("integration")} during the migration: the category lets
 * the legacy Surefire/Failsafe {@code excludedGroups}/{@code groups}
 * class-name selectors continue to bucket these tests correctly while the
 * Jupiter platform routes them via tag.
 * <p>
 * Spring's {@link SecurityContextHolder} is cleared after every test so
 * authentication state doesn't leak across runs (same contract as the JUnit 4
 * base; just expressed via {@code @AfterEach}).
 */
@Category(IntegrationTest.class)
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
