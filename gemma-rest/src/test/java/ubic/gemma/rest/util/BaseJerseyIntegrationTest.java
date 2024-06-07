package ubic.gemma.rest.util;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * Base class for Jersey-based integration tests.
 * <p>
 * Unfortunately, it's not possible to inherit from {@link ubic.gemma.core.util.test.BaseIntegrationTest} so we have to
 * duplicate some of the setup and teardown code.
 *
 * @author poirigui
 * @see ubic.gemma.core.util.test.BaseIntegrationTest
 */
@Category(IntegrationTest.class)
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" })
public abstract class BaseJerseyIntegrationTest extends BaseJerseyTest {

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

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
