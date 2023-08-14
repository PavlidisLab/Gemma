package ubic.gemma.rest.providers;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.support.GenericWebApplicationContext;
import ubic.gemma.rest.util.OpenApiUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class WebApplicationExceptionMapperTest extends JerseyTest {

    /**
     * This is a very simplisitc example that produces two representation for the same resource.
     */
    @Path("/custom")
    public static class CustomResource {

        public static class MyModel {

        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public MyModel indexAsJson() {
            throw new BadRequestException( "test" );
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String indexAsString() {
            throw new BadRequestException( "test" );
        }
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public Application configure() {
        return new ResourceConfig( CustomResource.class )
                .register( WebApplicationExceptionMapper.class )
                // otherwise jersey-spring3 will attempt to load the full Spring context
                .property( "contextConfig", new GenericWebApplicationContext() );
    }

    @Test
    public void testTextRepresentation() {
        assertThatThrownBy( () -> target( "/custom" ).request().accept( MediaType.TEXT_PLAIN ).get( CustomResource.MyModel.class ) )
                .isInstanceOf( BadRequestException.class )
                .extracting( "response" )
                .extracting( "entity" )
                .asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
                .hasContent( "Message: test" );
    }

    @Test
    public void testJsonRepresentation() {
        String version = OpenApiUtils.getOpenApi( new MockServletConfig() ).getInfo().getVersion();
        assertThatThrownBy( () -> target( "/custom" ).request().accept( MediaType.APPLICATION_JSON ).get( CustomResource.MyModel.class ) )
                .isInstanceOf( BadRequestException.class )
                .extracting( "response" )
                .extracting( "entity" )
                .asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
                .hasContent( String.format( "{\"error\":{\"code\":400,\"message\":\"test\"},\"apiVersion\":\"%s\"}", version ) );
    }
}