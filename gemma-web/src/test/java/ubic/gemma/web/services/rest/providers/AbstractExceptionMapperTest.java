package ubic.gemma.web.services.rest.providers;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AbstractExceptionMapperTest extends JerseyTest {

    @Provider
    private static class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {

        @Override
        protected Response.Status getStatus( WebApplicationException exception ) {
            return Response.Status.fromStatusCode( exception.getResponse().getStatus() );
        }
    }

    /**
     * This is a very simplisitc example that produces two representation for the same resource.
     */
    @Path("/")
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
        assertThatThrownBy( () -> target( "/" ).request().accept( MediaType.TEXT_PLAIN ).get( CustomResource.MyModel.class ) )
                .isInstanceOf( BadRequestException.class )
                .extracting( "response" )
                .extracting( "entity" )
                .asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
                .hasContent( "Message: test" );
    }

    @Test
    public void testJsonRepresentation() {
        assertThatThrownBy( () -> target( "/" ).request().accept( MediaType.APPLICATION_JSON ).get( CustomResource.MyModel.class ) )
                .isInstanceOf( BadRequestException.class )
                .extracting( "response" )
                .extracting( "entity" )
                .asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
                .hasContent( "{\"error\":{\"code\":400,\"message\":\"test\"},\"apiVersion\":\"2.5.0\"}" );
    }
}