package ubic.gemma.web.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.util.MailEngine;

import static org.mockito.Mockito.mock;

/**
 * Base class for a Web-based unit test.
 * <p>
 * For a full integration test base class, use {@link ubic.gemma.web.util.BaseWebIntegrationTest}.
 * @author poirigui
 */
@ActiveProfiles({ "web", EnvironmentProfiles.TEST })
@WebAppConfiguration
public abstract class BaseWebTest extends AbstractJUnit4SpringContextTests {

    public abstract static class BaseWebTestContextConfiguration {

        @Bean
        public HandlerExceptionResolver exceptionResolver() {
            return new DefaultHandlerExceptionResolver();
        }


        @Bean
        public MessageUtil messageUtil() {
            return mock( MessageUtil.class );
        }

        @Bean
        public MailEngine mailEngine() {
            return mock( MailEngine.class );
        }
    }

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mvc;

    /**
     * @see MockMvc#perform(RequestBuilder)
     */
    protected final ResultActions perform( RequestBuilder requestBuilder ) throws Exception {
        if ( mvc == null ) {
            mvc = MockMvcBuilders.webAppContextSetup( applicationContext ).build();
        }
        return mvc.perform( requestBuilder );
    }
}
