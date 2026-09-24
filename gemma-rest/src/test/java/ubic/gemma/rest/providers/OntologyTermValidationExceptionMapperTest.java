package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
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
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ubic.gemma.core.context.AsyncFactoryBean;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.TermViolation;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.OntologyTermValidationException;
import ubic.gemma.rest.util.OpenApiFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.rest.util.JsonAssert.json;

public class OntologyTermValidationExceptionMapperTest extends JerseyTest {

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
            return new OpenApiFactory( "ubic.gemma.rest.OntologyTermValidationExceptionMapperTest" );
        }

        @Bean
        public BuildInfo buildInfo() {
            return new BuildInfo();
        }
    }

    @Path("/custom")
    public static class CustomResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String index() {
            throw new OntologyTermValidationException( Arrays.asList(
                    new OntologyTermValidationException.Located( "tags[clientRef=t7].predicate",
                            new TermViolation( "predicate", "has_genotype", "http://x/00166", "delivered at dose", TermViolation.Reason.LABEL_MISMATCH ) ),
                    new OntologyTermValidationException.Located( "tags[clientRef=t7].object",
                            new TermViolation( "object", "Heterozygous", "http://x/00003", null, TermViolation.Reason.URI_UNRESOLVED ) )
            ) );
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
                .register( OntologyTermValidationExceptionMapper.class )
                .register( ObjectMapperResolver.class )
                .property( "contextConfig", ctx );
    }

    @Test
    public void testJsonRepresentation() {
        Response response = target( "/custom" ).request().accept( MediaType.APPLICATION_JSON ).get();
        assertThat( response.getStatus() ).isEqualTo( 400 );
        String body = response.readEntity( String.class );
        assertThat( body ).asInstanceOf( json() )
                .hasPathWithValue( "$.error.code", 400 )
                .hasPathWithValue( "$.error.errors[0].reason", "LABEL_MISMATCH" )
                .hasPathWithValue( "$.error.errors[0].location", "tags[clientRef=t7].predicate" )
                .hasPathWithValue( "$.error.errors[0].locationType", "BODY" )
                .hasPathWithValue( "$.error.errors[1].reason", "URI_UNRESOLVED" )
                .hasPathWithValue( "$.error.errors[1].location", "tags[clientRef=t7].object" );
    }
}
