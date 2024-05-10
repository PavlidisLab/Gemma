package ubic.gemma.web.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.OntologyIndividualSimple;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.web.util.BaseWebTest;
import ubic.gemma.web.util.EntityNotFoundException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration
public class OntologyControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class OntologyControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "url.gemmaOntology=http://gemma.msl.ubc.ca/ont/TGEMO.OWL",
                    "gemma.hosturl=https://gemma.msl.ubc.ca" );
        }

        @Bean
        public OntologyController ontologyController() {
            return new OntologyController();
        }

        @Bean
        public GemmaOntologyService gemmaOntology() {
            return mock();
        }

        @Bean
        public FactorValueOntologyService factorValueOntologyService() {
            return mock();
        }
    }

    @Value("${url.gemmaOntology}")
    private String gemmaOntologyUrl;

    @Autowired
    private GemmaOntologyService gemmaOntology;

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    @Before
    public void setUp() throws InterruptedException {
        when( gemmaOntology.isOntologyLoaded() ).thenReturn( true );
        when( gemmaOntology.getOntologyUrl() ).thenReturn( "http://gemma.msl.ubc.ca/ont/TGEMO.OWL" );
        when( gemmaOntology.getTerm( "http://gemma.msl.ubc.ca/ont/TGEMO_00001" ) )
                .thenReturn( new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGEMO_00001", "Homozygous negative" ) );
    }

    @After
    public void tearDown() {
        reset( gemmaOntology, factorValueOntologyService );
    }

    @Test
    public void testGemmaOntologyUnavailable() throws Exception {
        when( gemmaOntology.isOntologyLoaded() ).thenReturn( false );
        perform( get( "/ont/TGEMO_00001" ) )
                .andExpect( status().isServiceUnavailable() );
    }

    @Test
    public void testGetObo() throws Exception {
        perform( get( "/ont/TGEMO.OWL" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( gemmaOntologyUrl ) );
    }

    @Test
    public void testGetTerm() throws Exception {
        perform( get( "/ont/TGEMO_00001" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( gemmaOntologyUrl + "#http://gemma.msl.ubc.ca/ont/TGEMO_00001" ) );
    }

    @Test
    public void testGetMissingTerm() throws Exception {
        perform( get( "/ont/TGEMO_02312312" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attribute( "exception", instanceOf( EntityNotFoundException.class ) ) );
    }

    @Test
    public void testGetTgfvoAsHtml() throws Exception {
        OntologyTermSimple fvClass = new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGEMO_0000001", "bar" );
        OntologyIndividual oi = new OntologyIndividualSimple( "http://gemma.msl.ubc.ca/ont/TGFVO/1", "foo", fvClass );
        when( factorValueOntologyService.getIndividual( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ) )
                .thenReturn( oi );
        perform( get( "/ont/TGFVO/1" ) )
                .andExpect( status().isOk() )
                .andExpect( content().string( containsString( "FactorValue #1: foo" ) ) )
                .andExpect( content().string( containsString( "instance of" ) ) )
                .andExpect( content().string( containsString( "TGEMO_0000001" ) ) )
                .andExpect( content().string( containsString( "bar" ) ) )
                .andExpect( content().string( containsString( "curl -X Accept:application/rdf+xml https://gemma.msl.ubc.ca/ont/TGFVO/1" ) ) );
        verify( factorValueOntologyService ).getIndividual( "http://gemma.msl.ubc.ca/ont/TGFVO/1" );
        verify( factorValueOntologyService ).getFactorValueAnnotations( "http://gemma.msl.ubc.ca/ont/TGFVO/1" );
    }

    @Test
    public void testGetTgfvoAsRdf() throws Exception {
        OntologyTermSimple fvClass = new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGEMO_0000001", "bar" );
        OntologyIndividual oi = new OntologyIndividualSimple( "http://gemma.msl.ubc.ca/ont/TGFVO/1", "foo", fvClass );
        when( factorValueOntologyService.getIndividual( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ) )
                .thenReturn( oi );
        perform( get( "/ont/TGFVO/1" ).accept( MediaType.parseMediaType( "application/rdf+xml" ) ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( "application/rdf+xml" ) );
        verify( factorValueOntologyService ).getIndividual( "http://gemma.msl.ubc.ca/ont/TGFVO/1" );
        verify( factorValueOntologyService ).writeToRdf( eq( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ), any() );
        verifyNoMoreInteractions( factorValueOntologyService );
    }
}