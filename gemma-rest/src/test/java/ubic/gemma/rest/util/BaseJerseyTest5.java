package ubic.gemma.rest.util;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import ubic.gemma.core.context.EnvironmentProfiles;

import jakarta.ws.rs.core.Application;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseJerseyTest}.
 * <p>
 * Jersey 3.1's {@code jersey-test-framework-core} does not ship a Jupiter
 * extension (only the JUnit 4 base {@link JerseyTest} and a TestNG variant), so
 * the container lifecycle is bridged by overriding {@link JerseyTest#setUp()} /
 * {@link JerseyTest#tearDown()} and re-annotating them with
 * {@link BeforeEach} / {@link AfterEach}. The underlying methods on
 * {@link JerseyTest} are plain {@code public} methods (no JUnit 4 annotations
 * baked into the bytecode), so this re-annotation is the canonical
 * bridge pattern and does not double-fire the lifecycle.
 * <p>
 * Spring's {@link SpringExtension} replaces the JUnit 4
 * {@code SpringClassRule}/{@code SpringMethodRule} pair used in
 * {@link BaseJerseyTest}.
 *
 * @see BaseJerseyTest
 * @author poirigui
 */
@ActiveProfiles({ "web", EnvironmentProfiles.TEST })
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@TestExecutionListeners({ ServletTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class })
public abstract class BaseJerseyTest5 extends JerseyTest implements ApplicationContextAware {

    private ResourceConfig application;

    @Override
    protected final TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    protected final Application configure() {
        application = new ResourceConfig()
                .packages( "io.swagger.v3.jaxrs2.integration.resources", "ubic.gemma.rest" )
                .registerClasses( GZipEncoder.class )
                // use a generic context for now, it will be replaced when this bean is fully initialized in setApplicationContext()
                .property( "contextConfig", new GenericWebApplicationContext() )
                .property( "openApi.configuration.location", "/WEB-INF/classes/openapi-configuration.yaml" );
        return application;
    }

    @Override
    public final void setApplicationContext( ApplicationContext applicationContext ) {
        application.property( "contextConfig", applicationContext );
    }

    @Override
    protected final void configureClient( ClientConfig config ) {
        // ensures that the test client can decompress gzipped payloads
        config.register( GZipEncoder.class );
    }

    /**
     * Bridge the Jersey test-container setUp into Jupiter's lifecycle.
     * <p>
     * Intentionally {@code final} so subclasses cannot accidentally re-annotate
     * this with a conflicting {@code @BeforeEach} or override the container
     * bootstrap.
     */
    @BeforeEach
    @Override
    public final void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Bridge the Jersey test-container tearDown into Jupiter's lifecycle.
     */
    @AfterEach
    @Override
    public final void tearDown() throws Exception {
        super.tearDown();
    }
}
