package ubic.gemma.rest.util;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import ubic.gemma.persistence.util.EnvironmentProfiles;

import javax.ws.rs.core.Application;

/**
 * Base class for Jersey-based tests that needs a {@link WebApplicationContext} for loading and configuring or mocking
 * Spring components.
 * @author poirigui
 */
@ActiveProfiles({ "web", EnvironmentProfiles.TEST })
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public abstract class BaseJerseyTest extends JerseyTest implements InitializingBean {

    private ResourceConfig application;

    /**
     * The {@link WebApplicationContext} that is being used by the container. You can use it to inject specific beans
     * for testing purposes.
     */
    @Autowired
    private WebApplicationContext applicationContext;

    @Override
    protected final TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public final void afterPropertiesSet() {
        application.property( "contextConfig", applicationContext );
    }

    @Override
    protected final Application configure() {
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        application = new ResourceConfig()
                .packages( "io.swagger.v3.jaxrs2.integration.resources", "ubic.gemma.rest" )
                .registerClasses( GZipEncoder.class )
                // use a generic context for now, it will be replaced when this bean is fully initialized in afterPropertiesSet()
                .property( "contextConfig", new GenericWebApplicationContext() )
                .property( "openApi.configuration.location", "/WEB-INF/classes/openapi-configuration.yaml" );
        return application;
    }

    @Override
    protected final void configureClient( ClientConfig config ) {
        // ensures that the test client can decompress gzipped payloads
        config.register( GZipEncoder.class );
    }

    @Before
    @Override
    public final void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
