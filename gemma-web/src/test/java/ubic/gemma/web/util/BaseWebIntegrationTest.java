package ubic.gemma.web.util;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.core.util.test.category.IntegrationTest;

/**
 * Base class for web integration tests.
 * <p>
 * For a unit-test web test, use {@link BaseWebTest}.
 * @author poirigui
 */
@Category(IntegrationTest.class)
@ActiveProfiles("web")
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-*.xml" }),
        @ContextConfiguration(locations = { "classpath*:WEB-INF/gemma-servlet.xml" })
})
public abstract class BaseWebIntegrationTest extends BaseTest {

    @Autowired
    private WebApplicationContext applicationContext;

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

    private MockMvc mvc;

    protected final ResultActions perform( RequestBuilder requestBuilder ) throws Exception {
        if ( mvc == null ) {
            mvc = MockMvcBuilders.webAppContextSetup( applicationContext ).build();
        }
        return mvc.perform( requestBuilder );
    }
}
