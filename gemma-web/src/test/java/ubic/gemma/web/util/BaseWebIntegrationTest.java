package ubic.gemma.web.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ubic.gemma.core.util.test.BaseIntegrationTest;

/**
 * Base class for web integration tests.
 * <p>
 * For a unit-test web test, use {@link BaseWebTest}.
 * @author poirigui
 */
@ActiveProfiles("web")
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath*:WEB-INF/gemma-servlet.xml" })
public abstract class BaseWebIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mvc;

    protected final ResultActions perform( RequestBuilder requestBuilder ) throws Exception {
        if ( mvc == null ) {
            mvc = MockMvcBuilders.webAppContextSetup( applicationContext ).build();
        }
        return mvc.perform( requestBuilder );
    }
}
