package ubic.gemma.rest.providers;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.support.GenericWebApplicationContext;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code gemma.rest.rejectUnknownQueryParameters=false} restores the JAX-RS default of ignoring an undeclared
 * parameter. This is the rollback if enforcement turns out to break a client we could not see in advance, so it has
 * to actually work — separate container from {@link UnknownQueryParameterFilterTest} because the setting is fixed
 * when the filter is constructed.
 */
public class UnknownQueryParameterFilterDisabledTest extends JerseyTest {

    @Path("/custom")
    @Produces(MediaType.APPLICATION_JSON)
    public static class CustomResource {

        @GET
        public String index( @QueryParam("limit") String limit ) {
            return "\"ok\"";
        }
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public Application configure() {
        GenericWebApplicationContext ctx = new GenericWebApplicationContext();
        ctx.refresh();
        return new ResourceConfig( CustomResource.class )
                .register( new UnknownQueryParameterFilter( false ) )
                // jersey-spring6 otherwise bootstraps a context from applicationContext.xml; nothing here needs one
                .property( "contextConfig", ctx )
                .property( ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS, true );
    }

    @Test
    public void testUnknownParameterIsIgnoredWhenDisabled() {
        Response response = target( "/custom" )
                .queryParam( "limit", "10" )
                .queryParam( "banana", "42" )
                .request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
    }
}
