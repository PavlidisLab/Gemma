package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ubic.gemma.core.context.TestComponent;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.rest.util.JsonAssert.json;

/**
 * Pin the invariant that {@code /openapi.{json,yaml}} serves the very {@link OpenAPI} instance held by the
 * {@code openApi} bean — the instance {@code OpenApiFactory} decorates with the {@code servers} list, the
 * {@code FilterArg}/{@code SortArg} examples and placeholder resolution.
 * <p>
 * Swagger's {@code OpenApiResource} used to own this endpoint and answered from its own re-read of the spec, so a
 * request racing the factory's asynchronous build could pin an undecorated copy for the life of the JVM (frink
 * served a spec with no {@code servers} for 17 hours that way, which broke "Try it out" in the Swagger UI: with no
 * {@code servers} the UI falls back to the page origin and drops the {@code /rest/v2} base path). Marking the
 * decoration here with a server URL that only this test's bean carries is what makes the wiring, not just the
 * factory, the thing under test.
 *
 * @author poirigui
 */
public class OpenApiWebServiceTest extends JerseyTest {

    /**
     * A URL no other code path could have produced, so the assertions below can only pass if the response was
     * serialized from this bean's instance.
     */
    private static final String SERVER_URL = "https://openapi-web-service-test.example.org/rest/v2";

    @Configuration
    @TestComponent
    static class OpenApiWebServiceTestContextConfiguration {

        /**
         * Stand in for the {@code OpenApiFactory}-produced bean: an already-decorated spec behind a completed
         * future. Building a real spec would exercise the factory, which {@code OpenApiTest} already covers.
         */
        @Bean
        public Future<OpenAPI> openApi() {
            OpenAPI spec = new OpenAPI()
                    .info( new Info().title( "Gemma RESTful API" ).version( "1.2.3" ) )
                    .servers( Collections.singletonList( new Server().url( SERVER_URL ) ) );
            return CompletableFuture.completedFuture( spec );
        }
    }

    private static AnnotationConfigWebApplicationContext ctx;

    /**
     * Bridge Jersey's JUnit 4 lifecycle into Jupiter's, as in
     * {@link ubic.gemma.rest.providers.WebApplicationExceptionMapperTest}. {@code BaseJerseyTest5} is not reused
     * because its {@code configure()} is {@code final} and would pull in every resource in
     * {@code ubic.gemma.rest}.
     */
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
            ctx.register( OpenApiWebServiceTestContextConfiguration.class );
            ctx.refresh();
        }
        return new ResourceConfig( OpenApiWebService.class )
                // otherwise jersey-spring3 will attempt to load the full Spring context
                .property( "contextConfig", ctx );
    }

    @Test
    public void testJson() {
        Response response = target( "/openapi.json" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
        // compatibility rather than equality: Jersey appends a charset parameter for String entities
        assertThat( response.getMediaType().isCompatible( MediaType.APPLICATION_JSON_TYPE ) ).isTrue();
        assertThat( response.readEntity( String.class ) )
                .asInstanceOf( json() )
                .hasPathWithValue( "$.servers[0].url", SERVER_URL )
                .hasPathWithValue( "$.info.version", "1.2.3" );
    }

    @Test
    public void testYaml() {
        Response response = target( "/openapi.yaml" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
        assertThat( response.getMediaType().toString() ).startsWith( "application/yaml" );
        assertThat( response.readEntity( String.class ) ).contains( SERVER_URL );
    }
}