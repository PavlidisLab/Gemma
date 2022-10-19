package ubic.gemma.web.services.rest.util;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.ws.rs.core.Application;

/**
 * Base class for Jersey-based tests that needs a {@link WebApplicationContext} for loading and configuring or mocking
 * Spring components.
 * @author poirigui
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public abstract class BaseJerseyTest extends JerseyTest implements InitializingBean {

    private ResourceConfig application;

    /**
     * The {@link WebApplicationContext} that is being used by the container. You can use it to inject specific beans
     * for testing purposes.
     */
    @Autowired
    protected WebApplicationContext applicationContext;

    @Override
    protected final TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public void afterPropertiesSet() {
        application.property( "contextConfig", applicationContext );
    }

    @Override
    protected Application configure() {
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        application = new ResourceConfig()
                .packages( "io.swagger.v3.jaxrs2.integration.resources", "ubic.gemma.web.services.rest" )
                // use a generic context for now, it will be replaced when this bean is fully initialized in afterPropertiesSet()
                .property( "contextConfig", new GenericWebApplicationContext() )
                .property( "openApi.configuration.location", "/WEB-INF/classes/openapi-configuration.yaml" );
        return application;
    }
}
