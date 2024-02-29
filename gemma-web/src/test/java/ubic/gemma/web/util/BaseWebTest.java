package ubic.gemma.web.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.EnvironmentProfiles;

import java.util.Arrays;
import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * Base class for a Web-based unit test.
 * <p>
 * For a full integration test base class, use {@link ubic.gemma.web.util.BaseSpringWebTest}.
 * @author poirigui
 */
@ActiveProfiles({ "web", EnvironmentProfiles.TEST })
@WebAppConfiguration
public abstract class BaseWebTest extends AbstractJUnit4SpringContextTests implements InitializingBean {

    public abstract static class BaseWebTestContextConfiguration {

        @Bean
        public HandlerExceptionResolver exceptionResolver() {
            SimpleMappingExceptionResolver resolver = new SimpleMappingExceptionResolver();
            resolver.setDefaultErrorView( "error/500" );
            resolver.addStatusCode( "error/400", 400 );
            resolver.addStatusCode( "error/403", 403 );
            resolver.addStatusCode( "error/404", 404 );
            resolver.addStatusCode( "error/500", 500 );
            resolver.addStatusCode( "error/503", 503 );
            Properties mappings = new Properties();
            mappings.setProperty( AccessDeniedException.class.getName(), "error/403" );
            mappings.setProperty( EntityNotFoundException.class.getName(), "error/404" );
            mappings.setProperty( ServiceUnavailableException.class.getName(), "error/503" );
            mappings.setProperty( IllegalArgumentException.class.getName(), "error/400" );
            resolver.setExceptionMappings( mappings );
            HandlerExceptionResolverComposite compositeResolver = new HandlerExceptionResolverComposite();
            compositeResolver.setExceptionResolvers( Arrays.asList( resolver, unhandledExceptionResolver() ) );
            return compositeResolver;
        }

        private UnhandledExceptionResolver unhandledExceptionResolver() {
            UnhandledExceptionResolver resolver = new UnhandledExceptionResolver();
            resolver.setErrorCategory( "ubic.gemma.web.loggers.UnhandledException" );
            resolver.setStatusCode( 500 );
            resolver.setErrorView( "error/500" );
            return resolver;
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

    protected MockMvc mvc;

    @Override
    public final void afterPropertiesSet() {
        mvc = MockMvcBuilders.webAppContextSetup( applicationContext ).build();
    }
}
