package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ubic.gemma.core.context.AsyncFactoryBean;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.rest.annotations.AllowsUnknownQueryParameters;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.OpenApiFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.rest.util.JsonAssert.json;

/**
 * Pins the rejection of query parameters a resource method cannot bind, and — just as importantly — the four cases
 * that must keep working: a declared parameter, a path parameter, a route that reads the query string itself, and a
 * route that has been annotated to opt out.
 */
public class UnknownQueryParameterFilterTest extends JerseyTest {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    static class ContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.version=1.0.0", "gemma.build.timestamp=2024-05-20T04:41:58Z", "gemma.build.gitHash=1234", "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public AsyncFactoryBean<OpenAPI> openApi() {
            return new OpenApiFactory( "ubic.gemma.rest.UnknownQueryParameterFilterTest" );
        }

        @Bean
        public BuildInfo buildInfo() {
            return new BuildInfo();
        }
    }

    @Path("/custom")
    @Produces(MediaType.APPLICATION_JSON)
    public static class CustomResource {

        @GET
        public String index( @QueryParam("limit") String limit, @QueryParam("filter") String filter ) {
            return "\"ok\"";
        }

        @GET
        @Path("/none")
        public String none() {
            return "\"ok\"";
        }

        @GET
        @Path("/{id}/thing")
        public String withPathParam( @PathParam("id") String id, @QueryParam("limit") String limit ) {
            return "\"ok\"";
        }

        /**
         * Stands in for the 302 pass-through aliases, which forward every parameter they are given.
         */
        @GET
        @Path("/passthrough")
        @AllowsUnknownQueryParameters
        public String passthrough() {
            return "\"ok\"";
        }

        /**
         * Holds the raw {@link UriInfo}, so it can read any parameter and none can be shown to be ignored.
         */
        @GET
        @Path("/readsUriInfo")
        public String readsUriInfo( @Context UriInfo uriInfo ) {
            return "\"ok\"";
        }
    }

    private static AnnotationConfigWebApplicationContext ctx;

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
        if ( ctx == null ) {
            ctx = new AnnotationConfigWebApplicationContext();
            ctx.register( ContextConfiguration.class );
            ctx.refresh();
        }
        return new ResourceConfig( CustomResource.class )
                .register( new UnknownQueryParameterFilter( true ) )
                .register( UnknownQueryParameterExceptionMapper.class )
                .register( ObjectMapperResolver.class )
                .property( "contextConfig", ctx )
                .property( ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS, true );
    }

    @Test
    public void testDeclaredParametersAreAccepted() {
        Response response = target( "/custom" )
                .queryParam( "limit", "10" )
                .queryParam( "filter", "x" )
                .request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testNoParametersAtAllIsAccepted() {
        assertThat( target( "/custom" ).request().accept( MediaType.APPLICATION_JSON ).get().getStatus() )
                .isEqualTo( 200 );
    }

    /**
     * The reported case: {@code ids} is not a parameter of /datasets, was dropped, and the whole corpus came back
     * reported as the caller's selection.
     */
    @Test
    public void testUnknownParameterIsRejectedAndNamed() {
        Response response = target( "/custom" )
                .queryParam( "limit", "10" )
                .queryParam( "ids", "27103,1181" )
                .request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 400 );
        String body = response.readEntity( String.class );
        assertThat( body ).asInstanceOf( json() )
                .hasPathWithValue( "$.error.code", 400 )
                .hasPathWithValue( "$.error.errors[0].reason", "UNKNOWN_QUERY_PARAMETER" )
                .hasPathWithValue( "$.error.errors[0].location", "ids" )
                .hasPathWithValue( "$.error.errors[0].locationType", "QUERY" );
        // the message names the offender and then what would have worked
        assertThat( body ).contains( "Unknown query parameter 'ids'." )
                .contains( "This endpoint accepts: filter, limit." );
    }

    @Test
    public void testEveryUnknownParameterIsNamedNotJustTheFirst() {
        Response response = target( "/custom" )
                .queryParam( "banana", "42" )
                .queryParam( "kiwi", "7" )
                .request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 400 );
        String body = response.readEntity( String.class );
        assertThat( body ).asInstanceOf( json() )
                .hasPathWithValue( "$.error.errors[0].location", "banana" )
                .hasPathWithValue( "$.error.errors[1].location", "kiwi" );
        assertThat( body ).contains( "Unknown query parameters 'banana', 'kiwi'." );
    }

    @Test
    public void testEndpointDeclaringNoParametersSaysSo() {
        Response response = target( "/custom/none" )
                .queryParam( "limit", "10" )
                .request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 400 );
        assertThat( response.readEntity( String.class ) )
                .contains( "This endpoint does not accept any query parameter." );
    }

    /**
     * A path parameter is bound from the path, so it must not be mistaken for a declared query parameter, and its
     * presence must not widen what the query string may carry.
     */
    @Test
    public void testPathParameterIsNotAnAcceptedQueryParameter() {
        assertThat( target( "/custom/7/thing" ).queryParam( "limit", "1" )
                .request().accept( MediaType.APPLICATION_JSON ).get().getStatus() ).isEqualTo( 200 );
        Response response = target( "/custom/7/thing" ).queryParam( "id", "7" )
                .request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 400 );
        assertThat( response.readEntity( String.class ) ).contains( "Unknown query parameter 'id'." );
    }

    @Test
    public void testAnnotatedRouteAcceptsAnythingItIsGiven() {
        assertThat( target( "/custom/passthrough" ).queryParam( "anything", "at-all" )
                .request().accept( MediaType.APPLICATION_JSON ).get().getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testRouteHoldingUriInfoAcceptsAnythingItIsGiven() {
        assertThat( target( "/custom/readsUriInfo" ).queryParam( "anything", "at-all" )
                .request().accept( MediaType.APPLICATION_JSON ).get().getStatus() ).isEqualTo( 200 );
    }

    /**
     * A CORS preflight is sent to the URL of the request it is asking about, query string included, so rejecting it
     * would take the browser client down rather than the bad parameter.
     */
    @Test
    public void testPreflightIsNotRejected() {
        Response response = target( "/custom" ).queryParam( "banana", "42" ).request().options();
        assertThat( response.getStatus() ).isNotEqualTo( 400 );
    }
}
