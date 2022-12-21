package ubic.gemma.web.services.rest.util;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.TestAuthenticationUtils;

/**
 * Base class for Jersey-based integration tests.
 *
 * @author poirigui
 */
@ActiveProfiles({ "web", SpringProfiles.TEST })
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml",
        "classpath*:gemma/gsec/applicationContext-*.xml" })
public abstract class BaseJerseyIntegrationTest extends BaseJerseyTest {

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    @BeforeClass
    public static void setUpSecurityContextHolderStrategy() {
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
    }

    @Before
    public void setUpAuthentication() {
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
