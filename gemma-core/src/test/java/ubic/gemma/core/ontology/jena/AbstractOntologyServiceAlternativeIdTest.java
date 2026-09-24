package ubic.gemma.core.ontology.jena;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.OntologyService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link AbstractOntologyService#findUsingAlternativeId(String)} on the FIRST call, which is the one that has
 * to build the index.
 * <p>
 * The lookup used to read a stale local {@code State} after building the index into the field, so the very first
 * call for each ontology threw {@code NullPointerException: state.alternativeIDs is null} and every call after it
 * succeeded. An {@code assert} covered it, which meant nothing covered it — assertions are off in a deployed JVM.
 * It surfaced the first time a caller asked an ontology cold, from the obsolete-term report.
 * <p>
 * Real Jena parse of inlined OWL, no network.
 */
class AbstractOntologyServiceAlternativeIdTest {

    /**
     * An OBO merge as it is actually recorded: the surviving term carries the dead one's ID as
     * {@code hasAlternativeId}, and nothing is written on the dead term at all.
     */
    private static final String MERGED_OWL = "<?xml version=\"1.0\"?>\n" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
            "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n" +
            "         xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\">\n" +
            "  <owl:Ontology rdf:about=\"http://example.org/test\"/>\n" +
            "  <owl:Class rdf:about=\"http://purl.obolibrary.org/obo/TEST_0000002\">\n" +
            "    <rdfs:label>surviving term</rdfs:label>\n" +
            "    <oboInOwl:hasAlternativeId>TEST:0000001</oboInOwl:hasAlternativeId>\n" +
            "  </owl:Class>\n" +
            "</rdf:RDF>\n";

    private static OntologyService load() {
        UrlOntologyService svc = new UrlOntologyService( "test", "http://example.org/test", true, null );
        svc.setSearchEnabled( false );
        svc.setProcessImports( false );
        svc.initialize( new ByteArrayInputStream( MERGED_OWL.getBytes( StandardCharsets.UTF_8 ) ), false );
        return svc;
    }

    @Test
    void firstLookupBuildsTheIndexAndResolves() {
        OntologyService svc = load();

        // The first call is the whole point: it is the one that has to build the index and then read it back.
        OntologyTerm found = svc.findUsingAlternativeId( "TEST:0000001" );

        assertThat( found ).isNotNull();
        assertThat( found.getUri() ).isEqualTo( "http://purl.obolibrary.org/obo/TEST_0000002" );
        assertThat( found.getLabel() ).isEqualTo( "surviving term" );
    }

    @Test
    void repeatedLookupsStayConsistent() {
        OntologyService svc = load();

        OntologyTerm first = svc.findUsingAlternativeId( "TEST:0000001" );
        OntologyTerm second = svc.findUsingAlternativeId( "TEST:0000001" );

        assertThat( first ).isNotNull();
        assertThat( second ).isNotNull();
        assertThat( second.getUri() ).isEqualTo( first.getUri() );
    }

    @Test
    void unknownAlternativeIdIsNullRatherThanAnError() {
        OntologyService svc = load();

        assertThat( svc.findUsingAlternativeId( "TEST:9999999" ) ).isNull();
    }
}
