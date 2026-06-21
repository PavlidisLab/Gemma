package ubic.gemma.core.ontology.jena;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.providers.OntologyService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link AbstractOntologyService#getVersion()}: it must return {@code owl:versionInfo} when present,
 * fall back to {@code owl:versionIRI} otherwise, and yield {@code null} when neither is declared. Real Jena
 * parse of inlined OWL, no network — runs offline in well under a second. Backs the {@code ontologyVersion}
 * field surfaced on {@code /annotations/term}.
 */
class AbstractOntologyServiceVersionTest {

    private static String owl( String headerBody ) {
        return "<?xml version=\"1.0\"?>\n" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
                "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n" +
                "  <owl:Ontology rdf:about=\"http://example.org/test\">\n" +
                headerBody +
                "  </owl:Ontology>\n" +
                "  <owl:Class rdf:about=\"http://example.org/test#Foo\">\n" +
                "    <rdfs:label>foo</rdfs:label>\n" +
                "  </owl:Class>\n" +
                "</rdf:RDF>\n";
    }

    private static OntologyService load( String owlDoc ) {
        UrlOntologyService svc = new UrlOntologyService( "test", "http://example.org/test", true, null );
        svc.setSearchEnabled( false );
        svc.setProcessImports( false );
        svc.initialize( new ByteArrayInputStream( owlDoc.getBytes( StandardCharsets.UTF_8 ) ), false );
        return svc;
    }

    @Test
    void versionInfoIsPreferred() {
        OntologyService svc = load( owl( "    <owl:versionInfo>2024-05-29</owl:versionInfo>\n" ) );
        assertThat( svc.getVersion() ).isEqualTo( "2024-05-29" );
    }

    @Test
    void versionIriUsedWhenVersionInfoAbsent() {
        OntologyService svc = load( owl( "    <owl:versionIRI rdf:resource=\"http://example.org/test/releases/2024-05-29/test.owl\"/>\n" ) );
        assertThat( svc.getVersion() ).isEqualTo( "http://example.org/test/releases/2024-05-29/test.owl" );
    }

    @Test
    void versionIsNullWhenNeitherDeclared() {
        OntologyService svc = load( owl( "" ) );
        assertThat( svc.getVersion() ).isNull();
    }
}
