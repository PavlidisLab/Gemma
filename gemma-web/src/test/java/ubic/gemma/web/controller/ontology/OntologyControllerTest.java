package ubic.gemma.web.controller.ontology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.simple.OntologyIndividualSimple;
import ubic.basecode.ontology.simple.OntologyTermSimple;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.FileLockManagerImpl;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.web.controller.util.DownloadUtil;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.util.BaseWebTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration
public class OntologyControllerTest extends BaseWebTest {

    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;

    @Configuration
    @TestComponent
    static class OntologyControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "url.gemmaOntology=http://gemma.msl.ubc.ca/ont/TGEMO.OWL",
                    "gemma.hosturl=https://gemma.msl.ubc.ca",
                    "tomcat.sendfile.enabled=false",
                    "tgfvo.path=test" );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
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

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public DownloadUtil downloadUtil() {
            return new DownloadUtil();
        }

        @Bean
        public FileLockManager fileLockManager() {
            return new FileLockManagerImpl();
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

        AnnotationProperty annot = mock( AnnotationProperty.class );
        when( annot.getContents() ).thenReturn( "Double negative allele. Refers both to genetic null (missing gene/knock out) and phenotypic/functional null (inactive gene product). May be overloaded for severe loss-of-function alleles also." );
        when( annot.getProperty() ).thenReturn( "definition" );

        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getLabel() ).thenReturn( "Homozygous negative" );
        when( term.getUri() ).thenReturn( "http://gemma.msl.ubc.ca/ont/TGEMO_00001" );
        when( term.getLocalName() ).thenReturn( "TGEMO_00001" );
        when( term.getAnnotations() ).thenReturn( Collections.singletonList( annot ) );
        when( gemmaOntology.getTerm( "http://gemma.msl.ubc.ca/ont/TGEMO_00001" ) )
                .thenReturn( term );
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
    public void testGetGemmaOntologyHome() throws Exception {
        perform( get( "/ont/TGEMO" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "tgemo" ) )
                .andExpect( model().attributeExists( "terms" ) );
    }

    @Test
    public void testGetGemmaOntologyTerm() throws Exception {
        perform( get( "/ont/TGEMO_00001" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "tgemo.term" ) )
                .andExpect( model().attributeExists( "term" ) );
    }

    @Test
    public void testGetMissingTerm() throws Exception {
        perform( get( "/ont/TGEMO_02312312" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attribute( "exception", instanceOf( EntityNotFoundException.class ) ) );
    }

    @Test
    public void testGetTgfvoHome() throws Exception {
        when( factorValueOntologyService.getFactorValues( 0, 20 ) )
                .thenReturn( new Slice<>( Collections.emptyList(), null, 0, 20, 1000L ) );
        perform( get( "/ont/TGFVO" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "tgfvo" ) )
                .andExpect( model().attributeExists( "individuals" ) )
                .andExpect( model().attribute( "hostUrl", "https://gemma.msl.ubc.ca" ) )
                .andExpect( model().attribute( "offset", 0 ) )
                .andExpect( model().attribute( "limit", 20 ) )
                .andExpect( model().attribute( "count", 1000L ) );
        verify( factorValueOntologyService ).getFactorValues( 0, 20 );
    }

    @Test
    public void testGetTgfvoAsHtml() throws Exception {
        OntologyTermSimple fvClass = new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGEMO_0000001", "bar" );
        OntologyIndividual oi = new OntologyIndividualSimple( "http://gemma.msl.ubc.ca/ont/TGFVO/1", "foo", fvClass );
        when( factorValueOntologyService.getIndividual( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ) )
                .thenReturn( oi );
        perform( get( "/ont/TGFVO/1" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "tgfvo.factorValue" ) )
                .andExpect( model().attribute( "factorValueId", 1L ) )
                .andExpect( model().attribute( "oi", oi ) )
                .andExpect( model().attribute( "hostUrl", "https://gemma.msl.ubc.ca" ) );
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
        verify( factorValueOntologyService ).writeToRdf( eq( Collections.singleton( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ) ), any() );
        verifyNoMoreInteractions( factorValueOntologyService );
    }
}