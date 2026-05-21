package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.providers.CellLineOntologyService;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;
import ubic.gemma.core.ontology.basecode.search.OntologySearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Sets.set;

/**
 * Exercises the Lucene-9 ontology search index's stemming-with-exclusion behavior against a trimmed CLO fixture
 * (15-term subset of the full Cell Line Ontology, extracted via {@code robot filter --select "self annotations"}).
 * <p>
 * Loads the fixture from classpath instead of downloading the live CLO (~41 MB) so the test runs offline in &lt;5 s.
 * The over-the-wire equivalent is implicit in any {@code @Tag("integration")} ontology-load smoke test that
 * exercises the full corpus; this one is dedicated to the search-mechanism invariants.
 * <p>
 * The mechanism under test: when a word is added to the stem-exclusion set, it is indexed verbatim (no Porter
 * stemming applied), so a search for that exact word matches only docs whose label/synonym contains it verbatim,
 * AND a search for its stem does NOT pull those docs in. Conversely, a word NOT in the exclusion set is stemmed,
 * and queries for its stem expand to all morphological variants.
 */
public class CellLineOntologyTest {

    @Test
    public void test() throws OntologySearchException, IOException {
        CellLineOntologyService clo = new CellLineOntologyService();
        clo.setExcludedWordsFromStemming( set( "connectivity", "connective" ) );
        try ( InputStream is = new GZIPInputStream( new ClassPathResource( "/data/loader/ontology/clo.test.owl.gz" ).getInputStream() ) ) {
            clo.initialize( is, true );
        }

        // "connectivity" is in the stem-exclusion set → indexed verbatim → no fixture label contains the exact
        // word "connectivity" → empty result.
        assertThat( clo.findTerm( "connectivity", 500 ) ).isEmpty();

        // "connective" is in the stem-exclusion set → indexed verbatim → matches the literal "connective" labels
        // in the fixture (e.g. "connective tissue", "dense regular connective tissue") but DOES NOT pull in
        // "connect"-stem labels ("connects", "connected anatomical structure") because the stemmer doesn't see
        // either side as the same token.
        assertThat( clo.findTerm( "connective", 500 ) )
                .isNotEmpty()
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "dense regular connective tissue", "dense irregular connective tissue" )
                .doesNotContain( "connects", "connected anatomical structure" );

        // "connect" is NOT in the stem-exclusion set → indexed under its Porter stem, which is the same stem as
        // "connected"/"connects"/"connection". Result: matches the labels that contain those morphological
        // variants, but NOT the "connective" labels (which were protected from stemming).
        assertThat( clo.findTerm( "connect", 500 ) )
                .isNotEmpty()
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "connects" )
                .doesNotContain( "dense regular connective tissue", "dense irregular connective tissue" );

        // "connection" stems to the same Porter stem as "connect", so it must return the same kind of hits as
        // findTerm("connect") — i.e. the connect-stem labels, NOT the protected "connective" labels.
        assertThat( clo.findTerm( "connection", 500 ) )
                .isNotEmpty()
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .doesNotContain( "dense regular connective tissue", "dense irregular connective tissue" );
    }
}
