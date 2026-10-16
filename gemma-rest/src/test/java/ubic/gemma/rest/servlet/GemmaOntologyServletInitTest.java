package ubic.gemma.rest.servlet;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.OntologyExternalLinks;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link GemmaOntologyServlet#init()} against a real application context.
 * <p>
 * Gemma's settings are handed to the placeholder configurer ({@code SettingsConfig}) and are not in the
 * {@code Environment}, so reading {@code gemma.hosturl} from the environment failed the servlet's init on staging.
 */
class GemmaOntologyServletInitTest {

    @Configuration
    static class Config {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            Properties props = new Properties();
            props.setProperty( "gemma.hosturl", "https://staging-gemma.msl.ubc.ca" );
            configurer.setProperties( props );
            return configurer;
        }

        @Bean
        GemmaOntologyService gemmaOntologyService() {
            GemmaOntologyService s = mock( GemmaOntologyService.class );
            when( s.isOntologyLoaded() ).thenReturn( true );
            when( s.getAllURIs() ).thenReturn( Collections.emptySet() );
            return s;
        }

        @Bean
        FactorValueOntologyService factorValueOntologyService() {
            return mock( FactorValueOntologyService.class );
        }

        @Bean
        ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "https://staging-gemma.msl.ubc.ca" );
        }

        @Bean
        OntologyExternalLinks ontologyExternalLinks() throws IOException {
            return new OntologyExternalLinks( false );
        }
    }

    @Test
    void hostUrlComesFromThePlaceholderConfigurer() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        try ( AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext() ) {
            ctx.setServletContext( servletContext );
            ctx.register( Config.class );
            ctx.refresh();
            assertThat( ctx.getEnvironment().getProperty( "gemma.hosturl" ) ).isNull();
            servletContext.setAttribute( WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx );

            GemmaOntologyServlet servlet = new GemmaOntologyServlet();
            servlet.init( new MockServletConfig( servletContext ) );

            MockHttpServletRequest req = new MockHttpServletRequest( "GET", "/ont/TGEMO" );
            req.setServletPath( "/ont" );
            req.setPathInfo( "/TGEMO" );
            MockHttpServletResponse res = new MockHttpServletResponse();
            servlet.doGet( req, res );
            assertThat( res.getStatus() ).isEqualTo( 200 );
            assertThat( res.getContentAsString() ).contains( "https://staging-gemma.msl.ubc.ca/ont/TGEMO.OWL" );
        }
    }
}
