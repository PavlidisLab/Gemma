package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Reproduces the 500 reported in
 * {@code CAB_EVAL_TO_GEMBRO_2026_09_06_A_SLASH_AND_A_PAREN_IN_ONE_QUERY_IS_A_500_ON_TWO_PUBLIC_SEARCH_ENDPOINTS.md}
 * at the ontology index, which is the {@code /annotations/search} half of it.
 *
 * <h2>Why the existing escape-retry did not cover this</h2>
 *
 * <p>An unescaped {@code /} opens a Lucene regex term. An <em>unterminated</em> one
 * ({@code 100 ng/ml}) makes the lexer throw {@code TokenMgrError} and recovers. A
 * <em>terminated</em> one whose body is not a valid regex does not: {@code a/b (c/d)} lexes
 * cleanly, and {@code RegExp} then rejects the body {@code b (c} with an
 * {@link IllegalArgumentException} — a third type, caught by neither arm of the fallback, which
 * left the endpoint answering 500 for ordinary strain and dose text.</p>
 */
class LuceneOntologySearchIndexRegexTest {

    private static final String OWL = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n"
            + "  <owl:Class rdf:about=\"http://example.org/T_1\">\n"
            + "    <rdfs:label>C57BL/6 (H-2b/d)</rdfs:label>\n"
            + "  </owl:Class>\n"
            + "</rdf:RDF>\n";

    private static OntModel model() {
        OntModel m = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        m.read( new ByteArrayInputStream( OWL.getBytes( StandardCharsets.UTF_8 ) ), null );
        return m;
    }

    @Test
    void queryWithParenBetweenSlashesDoesNotThrow() throws Exception {
        OntModel m = model();
        try ( SearchIndex idx = OntologyIndexer.indexOntology( "test", m, Collections.emptySet(), true ) ) {
            assertThatNoException().isThrownBy( () -> idx.search( m, "a/b (c/d)", 10 ) );
        }
    }

    @Test
    void aStrainNameWithSlashesAndParensStillMatchesItsTerm() throws Exception {
        OntModel m = model();
        try ( SearchIndex idx = OntologyIndexer.indexOntology( "test", m, Collections.emptySet(), true ) ) {
            List<SearchIndex.JenaSearchResult> hits = idx.search( m, "C57BL/6 (H-2b/d)", 10 );
            assertThat( hits ).isNotEmpty();
            assertThat( hits.get( 0 ).result.getURI() ).isEqualTo( "http://example.org/T_1" );
        }
    }
}
