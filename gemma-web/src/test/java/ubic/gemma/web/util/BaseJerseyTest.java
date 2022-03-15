package ubic.gemma.web.util;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Before;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import ubic.gemma.core.util.test.AuthenticationTestingUtil;

import javax.ws.rs.core.Application;

/**
 * Base class for Jersey-based integration tests.
 *
 * Note that beans injection is not supported in the test because we were forced to choose {@link JerseyTest} over
 * {@link org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests} as a base class.
 *
 * You can use {@link #applicationContext} to inject beans instead.
 *
 * @author poirigui
 */
public abstract class BaseJerseyTest extends JerseyTest {

    /**
     * The {@link WebApplicationContext} that is being used by the container. You can use it to inject specific beans
     * for testing purposes.
     */
    protected WebApplicationContext applicationContext;

    protected AuthenticationTestingUtil authenticationTestingUtil;

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public Application configure() {
        applicationContext = prepareWebApplicationContext();
        authenticationTestingUtil = applicationContext.getBean( AuthenticationTestingUtil.class );
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        return new ResourceConfig()
                .packages( "io.swagger.v3.jaxrs2.integration.resources", "ubic.gemma.web.services.rest" )
                .property( "contextConfig", applicationContext )
                .property( "openApi.configuration.location", "/WEB-INF/classes/openapi-configuration.yaml" );
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        authenticationTestingUtil.grantAdminAuthority( applicationContext );
    }

    private WebApplicationContext prepareWebApplicationContext() {
        XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
        applicationContext.setConfigLocations( new String[] {
                "classpath*:ubic/gemma/applicationContext-*.xml",
                "classpath*:gemma/gsec/applicationContext-*.xml",
                "classpath:ubic/gemma/testDataSource.xml" } );
        applicationContext.setServletConfig( new MockServletConfig() );
        applicationContext.setServletContext( new MockServletContext() );
        applicationContext.refresh();
        return applicationContext;
    }
}
