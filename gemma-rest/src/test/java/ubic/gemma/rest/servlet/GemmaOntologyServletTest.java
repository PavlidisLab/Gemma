package ubic.gemma.rest.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.OntologyExternalLinks;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.simple.OntologyIndividualSimple;
import ubic.gemma.core.ontology.simple.OntologyPropertySimple;
import ubic.gemma.core.ontology.simple.OntologyStatementSimple;
import ubic.gemma.core.ontology.simple.OntologyTermSimple;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;
import ubic.gemma.persistence.util.Slice;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GemmaOntologyServletTest {

    private static final String PREFIX = "http://gemma.msl.ubc.ca/ont/";
    private static final String TGFVO = PREFIX + "TGFVO/";

    private GemmaOntologyService gemmaOntologyService;
    private FactorValueOntologyService factorValueOntologyService;
    private ExpressionExperimentService expressionExperimentService;
    private GemmaOntologyServlet servlet;

    @BeforeEach
    public void setUp() throws IOException {
        gemmaOntologyService = mock( GemmaOntologyService.class );
        when( gemmaOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( gemmaOntologyService.getOntologyUrl() ).thenReturn( "https://example.org/TGEMO.OWL" );
        OntologyTerm parent = term( "TGEMO_00001", "parent term" );
        OntologyTerm term = term( "TGEMO_00184", "delivered <at> dose" );
        AnnotationProperty definition = mock( AnnotationProperty.class );
        when( definition.getProperty() ).thenReturn( "definition" );
        when( definition.getContents() ).thenReturn( "A definition." );
        when( term.getAnnotations() ).thenReturn( Collections.singleton( definition ) );
        when( term.getParents( true ) ).thenReturn( Collections.singleton( parent ) );
        when( parent.getChildren( true ) ).thenReturn( Collections.singleton( term ) );
        // the real URI set contains null (owl:Restriction class expressions) next to IRIs from other ontologies
        when( gemmaOntologyService.getAllURIs() ).thenReturn( new HashSet<>( Arrays.asList(
                PREFIX + "TGEMO_00001", PREFIX + "TGEMO_00184", "http://purl.obolibrary.org/obo/UBERON_0000955", null ) ) );

        factorValueOntologyService = mock( FactorValueOntologyService.class );
        expressionExperimentService = mock( ExpressionExperimentService.class );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 7L );
        ee.setShortName( "GSE1234" );
        when( expressionExperimentService.findByFactorValueId( 12L ) ).thenReturn( ee );
        when( expressionExperimentService.findByFactorValueId( 13L ) ).thenThrow( new AccessDeniedException( "private" ) );
        OntologyTermSimple genotype = new OntologyTermSimple( "http://www.ebi.ac.uk/efo/EFO_0000513", "genotype" );
        OntologyIndividualSimple fv = new OntologyIndividualSimple( TGFVO + "12", "wild type", genotype );
        OntologyIndividualSimple annotation = new OntologyIndividualSimple( TGFVO + "12/1", "wild type genotype",
                new OntologyTermSimple( "http://www.ebi.ac.uk/efo/EFO_0005168", "wild type genotype" ) );
        when( factorValueOntologyService.getIndividual( TGFVO + "12" ) ).thenReturn( fv );
        when( factorValueOntologyService.getIndividual( TGFVO + "12/1" ) ).thenReturn( annotation );
        when( factorValueOntologyService.getFactorValueAnnotations( TGFVO + "12" ) ).thenReturn( Collections.singleton( annotation ) );
        when( factorValueOntologyService.getFactorValueStatements( TGFVO + "12" ) ).thenReturn( Collections.singleton(
                new OntologyStatementSimple( annotation, new OntologyPropertySimple( "http://purl.obolibrary.org/obo/RO_0002200", "has phenotype" ),
                        new OntologyIndividualSimple( TGFVO + "12/2", "normal", null ) ) ) );
        when( factorValueOntologyService.getFactorValues( anyInt(), anyInt() ) ).thenAnswer( a -> {
            int offset = a.getArgument( 0 ), limit = a.getArgument( 1 );
            return new Slice<>( Collections.singletonList( fv ), null, offset, limit, 45L );
        } );
        when( factorValueOntologyService.getFactorValueUris( anyInt(), anyInt() ) ).thenAnswer( a ->
                new Slice<>( Collections.singletonList( TGFVO + "12" ), null, a.getArgument( 0 ), a.getArgument( 1 ), 45L ) );

        servlet = new GemmaOntologyServlet( gemmaOntologyService, factorValueOntologyService, expressionExperimentService,
                new EntityUrlBuilder( "https://gemma.msl.ubc.ca" ), new OntologyExternalLinks( false ), "https://gemma.msl.ubc.ca" );
    }

    private OntologyTerm term( String localName, String label ) {
        OntologyTerm t = mock( OntologyTerm.class );
        when( t.getUri() ).thenReturn( PREFIX + localName );
        when( t.getLocalName() ).thenReturn( localName );
        when( t.getLabel() ).thenReturn( label );
        when( gemmaOntologyService.getTerm( PREFIX + localName ) ).thenReturn( t );
        return t;
    }

    private MockHttpServletResponse get( String pathInfo ) throws IOException {
        return get( pathInfo, null, null );
    }

    private MockHttpServletResponse get( String pathInfo, String query, String accept ) throws IOException {
        MockHttpServletRequest req = new MockHttpServletRequest( "GET", "/ont" + pathInfo );
        req.setServletPath( "/ont" );
        req.setPathInfo( pathInfo );
        if ( query != null ) {
            req.setQueryString( query );
            for ( String kv : query.split( "&" ) ) {
                String[] p = kv.split( "=", 2 );
                req.addParameter( p[0], p[1] );
            }
        }
        if ( accept != null ) {
            req.addHeader( "Accept", accept );
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        servlet.doGet( req, res );
        return res;
    }

    @Test
    public void testHome() throws IOException {
        MockHttpServletResponse res = get( "/TGEMO" );
        assertThat( res.getStatus() ).isEqualTo( 200 );
        assertThat( res.getContentType() ).startsWith( "text/html" );
        assertThat( res.getContentAsString() )
                .contains( "<a href=\"/ont/TGEMO_00001\">parent term</a>" )
                .contains( "<a href=\"/ont/TGEMO_00184\">delivered &lt;at&gt; dose</a>" )
                .contains( "https://gemma.msl.ubc.ca/ont/TGEMO.OWL" )
                .contains( "<a href=\"/ont/TGFVO\">" )
                // only TGEMO IRIs are listed
                .doesNotContain( "UBERON" );
    }

    @Test
    public void testOwlRedirectsToPublishedFile() throws IOException {
        assertThat( get( "/TGEMO.OWL" ).getRedirectedUrl() ).isEqualTo( "https://example.org/TGEMO.OWL" );
    }

    @Test
    public void testTerm() throws IOException {
        MockHttpServletResponse res = get( "/TGEMO_00184" );
        assertThat( res.getStatus() ).isEqualTo( 200 );
        assertThat( res.getContentAsString() )
                .contains( "<title>delivered &lt;at&gt; dose</title>" )
                .contains( "<p>A definition.</p>" )
                .contains( PREFIX + "TGEMO_00184" )
                // the parent comes before the term in the hierarchy
                .containsSubsequence( "parent term", "<strong><a href=\"/ont/TGEMO_00184\">" );
    }

    @Test
    public void testTermCurie() throws IOException {
        assertThat( get( "/TGEMO:00184" ).getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testUnknownTerm() throws IOException {
        assertThat( get( "/TGEMO_99999" ).getStatus() ).isEqualTo( 404 );
    }

    @Test
    public void testNotLoaded() throws IOException {
        when( gemmaOntologyService.isOntologyLoaded() ).thenReturn( false );
        assertThat( get( "/TGEMO_00184" ).getStatus() ).isEqualTo( 503 );
        assertThat( get( "/TGEMO" ).getStatus() ).isEqualTo( 503 );
        // the OWL redirect does not need the loaded ontology, nor does TGFVO
        assertThat( get( "/TGEMO.OWL" ).getStatus() ).isEqualTo( 302 );
        assertThat( get( "/TGFVO/12" ).getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testRootRedirectsToHome() throws IOException {
        assertThat( get( "/" ).getRedirectedUrl() ).isEqualTo( "/ont/TGEMO" );
    }

    @Test
    public void testTgfvoOwlIsNotServed() throws IOException {
        assertThat( get( "/TGFVO.OWL" ).getStatus() ).isEqualTo( 404 );
    }

    @Test
    public void testFactorValues() throws IOException {
        MockHttpServletResponse res = get( "/TGFVO", "offset=25&limit=10", null );
        assertThat( res.getStatus() ).isEqualTo( 200 );
        verify( factorValueOntologyService ).getFactorValues( 20, 10 ); // rounded down to the start of the page
        assertThat( res.getContentAsString() )
                .contains( "<a href=\"/ont/TGFVO/12\">wild type</a>" )
                .contains( "<a href=\"?offset=0&amp;limit=10\">First page</a>" )
                .contains( "<a href=\"?offset=10&amp;limit=10\">Previous page</a>" )
                // the old page linked "Next" to the current offset
                .contains( "<a href=\"?offset=30&amp;limit=10\">Next page</a>" )
                .contains( "<a href=\"?offset=40&amp;limit=10\">Last page</a>" )
                .contains( "21–30 of 45" )
                .doesNotContain( "TGFVO.OWL" );
        assertThat( res.getHeader( "Vary" ) ).isEqualTo( "Accept" );
    }

    @Test
    public void testFactorValuesLastPage() throws IOException {
        assertThat( get( "/TGFVO", "offset=40&limit=10", null ).getContentAsString() )
                .contains( "<span>Next page</span>" )
                .contains( "<span>Last page</span>" )
                .contains( "41–45 of 45" );
    }

    @Test
    public void testFactorValuesBadParameters() throws IOException {
        assertThat( get( "/TGFVO", "limit=101", null ).getStatus() ).isEqualTo( 400 );
        assertThat( get( "/TGFVO", "limit=0", null ).getStatus() ).isEqualTo( 400 );
        assertThat( get( "/TGFVO", "offset=-1", null ).getStatus() ).isEqualTo( 400 );
        assertThat( get( "/TGFVO", "offset=abc", null ).getStatus() ).isEqualTo( 400 );
    }

    @Test
    public void testFactorValuesAsRdf() throws IOException {
        MockHttpServletResponse res = get( "/TGFVO", null, "application/rdf+xml" );
        assertThat( res.getContentType() ).startsWith( "application/rdf+xml" );
        // the page of IDs is already filtered by ACL
        verify( factorValueOntologyService ).writeToRdfIgnoreAcls( eq( Collections.singletonList( TGFVO + "12" ) ), any( Writer.class ) );
    }

    @Test
    public void testFactorValue() throws IOException {
        MockHttpServletResponse res = get( "/TGFVO/12" );
        assertThat( res.getStatus() ).isEqualTo( 200 );
        assertThat( res.getContentAsString() )
                .contains( "<title>FactorValue #12: wild type</title>" )
                .contains( "<li>belongs to <a href=\"https://gemma.msl.ubc.ca/#/dataset/7\">GSE1234</a></li>" )
                .contains( "instance of <a href=\"https://www.ebi.ac.uk/ols4/ontologies/efo/classes/?short_form=EFO_0000513\"" )
                .contains( "has annotation <a href=\"/ont/TGFVO/12/1\">wild type genotype</a>" )
                .containsSubsequence( "<a href=\"/ont/TGFVO/12/1\">", "has phenotype", "<a href=\"/ont/TGFVO/12/2\">normal</a>" )
                .contains( "curl -H Accept:application/rdf+xml https://gemma.msl.ubc.ca/ont/TGFVO/12" );
    }

    @Test
    public void testFactorValueAsRdf() throws IOException {
        MockHttpServletResponse res = get( "/TGFVO/12", null, "application/rdf+xml, text/html;q=0.5" );
        assertThat( res.getContentType() ).startsWith( "application/rdf+xml" );
        // a single factor value is checked against ACLs
        verify( factorValueOntologyService ).writeToRdf( eq( Collections.singleton( TGFVO + "12" ) ), any( Writer.class ) );
        verify( factorValueOntologyService, never() ).writeToRdfIgnoreAcls( any(), any() );
    }

    @Test
    public void testWildcardAcceptGetsHtml() throws IOException {
        assertThat( get( "/TGFVO/12", null, "*/*" ).getContentType() ).startsWith( "text/html" );
        assertThat( get( "/TGFVO/12", null, "not a media type" ).getContentType() ).startsWith( "text/html" );
    }

    @Test
    public void testFactorValueAnnotation() throws IOException {
        MockHttpServletResponse res = get( "/TGFVO/12/1" );
        assertThat( res.getStatus() ).isEqualTo( 200 );
        assertThat( res.getContentAsString() )
                .contains( "<title>Annotation #1 of FactorValue #12: wild type genotype</title>" )
                .contains( "annotation of <a href=\"/ont/TGFVO/12\">wild type</a>" )
                .contains( "https://gemma.msl.ubc.ca/#/dataset/7" );
    }

    @Test
    public void testFactorValueOfPrivateDatasetIsNotFound() throws IOException {
        assertThat( get( "/TGFVO/13" ).getStatus() ).isEqualTo( 404 );
        assertThat( get( "/TGFVO/13", null, "application/rdf+xml" ).getStatus() ).isEqualTo( 404 );
        verify( factorValueOntologyService, never() ).getIndividual( any() );
        verify( factorValueOntologyService, never() ).writeToRdf( any(), any() );
    }

    @Test
    public void testUnknownFactorValue() throws IOException {
        // no dataset
        assertThat( get( "/TGFVO/99" ).getStatus() ).isEqualTo( 404 );
        // a dataset, but no such annotation
        assertThat( get( "/TGFVO/12/99" ).getStatus() ).isEqualTo( 404 );
        assertThat( get( "/TGFVO/abc" ).getStatus() ).isEqualTo( 404 );
    }
}
