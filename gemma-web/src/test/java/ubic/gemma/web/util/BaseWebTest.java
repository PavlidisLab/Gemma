package ubic.gemma.web.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.SpringProfiles;

import static org.mockito.Mockito.mock;

/**
 * Base class for a Web-based unit test.
 * <p>
 * For a full integration test base class, use {@link ubic.gemma.web.util.BaseSpringWebTest}.
 * @author poirigui
 */
@ActiveProfiles({ "web", SpringProfiles.TEST })
@WebAppConfiguration
public abstract class BaseWebTest extends AbstractJUnit4SpringContextTests implements InitializingBean {

    public abstract static class BaseWebTestContextConfiguration {

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

    protected MockMvc mvc;

    @Override
    public final void afterPropertiesSet() {
        mvc = MockMvcBuilders.webAppContextSetup( applicationContext ).build();
    }
}
