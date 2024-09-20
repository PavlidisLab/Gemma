package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.OpenApiFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class WebApplicationExceptionMapperTest extends JerseyTest {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    static class WebApplicationExceptionMapperTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.version=1.0.0", "gemma.build.timestamp=2024-05-20T04:41:58Z", "gemma.build.gitHash=1234", "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public FactoryBean<OpenAPI> openApi() {
            return new OpenApiFactory( "ubic.gemma.rest.WebApplicationExceptionMapperTest" );
        }

        @Bean
        public BuildInfo buildInfo() {
            return new BuildInfo();
        }
    }

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

    private static AnnotationConfigWebApplicationContext ctx;

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    public Application configure() {
        if ( ctx == null ) {
            ctx = new AnnotationConfigWebApplicationContext();
            ctx.register( WebApplicationExceptionMapperTestContextConfiguration.class );
            ctx.refresh();
        }
        return new ResourceConfig( CustomResource.class )
                .register( WebApplicationExceptionMapper.class )
                .register( ObjectMapperResolver.class )
                // otherwise jersey-spring3 will attempt to load the full Spring context
                .property( "contextConfig", ctx );
    }

    @Test
    public void testTextRepresentation() {
        String version = ctx.getBean( OpenAPI.class ).getInfo().getVersion();
        BuildInfo buildInfo = ctx.getBean( BuildInfo.class );
        assertThatThrownBy( () -> target( "/custom" ).request().accept( MediaType.TEXT_PLAIN ).get( CustomResource.MyModel.class ) )
                .isInstanceOf( BadRequestException.class )
                .extracting( "response" )
                .extracting( "entity" )
                .asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
                .hasContent( String.format( "Request method: GET\nRequest URI: http://localhost:8080/custom\nVersion: %s\nBuild info: %s\nMessage: test", version, buildInfo ) );
    }

    @Test
    public void testJsonRepresentation() {
        assertThatThrownBy( () -> target( "/custom" ).request().accept( MediaType.APPLICATION_JSON ).get( CustomResource.MyModel.class ) )
                .isInstanceOf( BadRequestException.class )
                .extracting( "response" )
                .extracting( "entity" )
                .asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
                .hasContent( "\"{\"apiVersion\":\"2.8.3\",\"buildInfo\":{\"version\":\"\",\"timestamp\":\"2024-05-20T04:41:58.000+00:00\",\"gitHash\":\"1234\"},\"error\":{\"code\":400,\"message\":\"test\"}}\"" );
    }
}