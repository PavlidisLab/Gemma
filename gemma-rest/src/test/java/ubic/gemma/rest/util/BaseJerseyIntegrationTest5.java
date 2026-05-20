package ubic.gemma.rest.util;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseJerseyIntegrationTest}.
 * <p>
 * Carries both the JUnit 4 {@code @Category(IntegrationTest.class)} and the
 * JUnit 5 {@code @Tag("integration")} during the migration so that the legacy
 * Surefire/Failsafe class-name selectors keep bucketing these tests while the
 * Jupiter platform routes them by tag (same dual-marker pattern as
 * {@link ubic.gemma.core.util.test.BaseIntegrationTest5}).
 *
 * @see BaseJerseyIntegrationTest
 * @author poirigui
 */
@Category(IntegrationTest.class)
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
