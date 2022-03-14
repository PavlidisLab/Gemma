package ubic.gemma.web.util;

import gemma.gsec.authentication.UserManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
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
public abstract class BaseSpringWebJerseyTest extends JerseyTest {

    /**
     * The {@link WebApplicationContext} that is being used by the container. You can use it to inject specific beans
     * for testing purposes.
     */
    protected WebApplicationContext applicationContext;

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public Application configure() {
        applicationContext = prepareWebApplicationContext();
        return new ResourceConfig()
                .packages( "ubic.gemma.web.services.rest" )
                .property( "contextConfig", applicationContext );
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
        // setup basic credentials
        AuthenticationTestingUtil authenticationTestingUtil = new AuthenticationTestingUtil( applicationContext.getBean( UserManager.class ) );
        authenticationTestingUtil.grantAdminAuthority( applicationContext );
        return applicationContext;
    }
}
