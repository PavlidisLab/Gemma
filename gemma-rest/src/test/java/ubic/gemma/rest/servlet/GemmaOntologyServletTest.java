package ubic.gemma.rest.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.core.ontology.OntologyExternalLinks;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class GemmaOntologyServletTest {

    private static final String PREFIX = "http://gemma.msl.ubc.ca/ont/";

    private GemmaOntologyService gemmaOntologyService;
    private GemmaOntologyServlet servlet;
    private OntologyTerm parent, term;

    @BeforeEach
    public void setUp() throws IOException {
        gemmaOntologyService = mock( GemmaOntologyService.class );
        when( gemmaOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( gemmaOntologyService.getOntologyUrl() ).thenReturn( "https://example.org/TGEMO.OWL" );
        parent = term( "TGEMO_00001", "parent term" );
        term = term( "TGEMO_00184", "delivered <at> dose" );
        AnnotationProperty definition = mock( AnnotationProperty.class );
        when( definition.getProperty() ).thenReturn( "definition" );
        when( definition.getContents() ).thenReturn( "A definition." );
        when( term.getAnnotations() ).thenReturn( Collections.singleton( definition ) );
        when( term.getParents( true ) ).thenReturn( Collections.singleton( parent ) );
        when( parent.getChildren( true ) ).thenReturn( Collections.singleton( term ) );
        when( gemmaOntologyService.getAllURIs() ).thenReturn( new HashSet<>( Arrays.asList(
                PREFIX + "TGEMO_00001", PREFIX + "TGEMO_00184", "http://purl.obolibrary.org/obo/UBERON_0000955" ) ) );
        servlet = new GemmaOntologyServlet( gemmaOntologyService, new OntologyExternalLinks( false ), "https://gemma.msl.ubc.ca" );
    }

    private OntologyTerm term( String localName, String label ) {
        OntologyTerm t = mock( OntologyTerm.class );
        if ( localName != null ) {
            when( t.getUri() ).thenReturn( PREFIX + localName );
            when( t.getLocalName() ).thenReturn( localName );
            when( gemmaOntologyService.getTerm( PREFIX + localName ) ).thenReturn( t );
        }
        when( t.getLabel() ).thenReturn( label );
        return t;
    }

    private MockHttpServletResponse get( String pathInfo ) throws IOException {
        MockHttpServletRequest req = new MockHttpServletRequest( "GET", "/ont" + pathInfo );
        req.setServletPath( "/ont" );
        req.setPathInfo( pathInfo );
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
    public void testTgfvoNotServedYet() throws IOException {
        assertThat( get( "/TGFVO/123" ).getStatus() ).isEqualTo( 404 );
    }

    @Test
    public void testNotLoaded() throws IOException {
        when( gemmaOntologyService.isOntologyLoaded() ).thenReturn( false );
        assertThat( get( "/TGEMO_00184" ).getStatus() ).isEqualTo( 503 );
        assertThat( get( "/TGEMO" ).getStatus() ).isEqualTo( 503 );
        // the OWL redirect does not need the loaded ontology
        assertThat( get( "/TGEMO.OWL" ).getStatus() ).isEqualTo( 302 );
    }

    @Test
    public void testRootRedirectsToHome() throws IOException {
        assertThat( get( "/" ).getRedirectedUrl() ).isEqualTo( "/ont/TGEMO" );
    }
}
