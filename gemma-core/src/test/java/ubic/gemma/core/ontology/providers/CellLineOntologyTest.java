package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.CellLineOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.util.test.category.SlowTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Sets.set;

public class CellLineOntologyTest {

    @Test
    @Category(SlowTest.class)
    public void test() throws OntologySearchException {
        CellLineOntologyService clo = new CellLineOntologyService();
        clo.setExcludedWordsFromStemming( set( "connectivity", "connective" ) );
        clo.initialize( true, true );
        // NOTE: this test loads the live CLO via the http://purl.obolibrary.org/obo/clo.owl PURL, whose redirect
        // target and contents change with each release (e.g. the artifact was republished on GitHub on 2026-06-20,
        // dropping several terms). Exact match counts therefore drift, so we assert lower bounds plus the specific
        // terms whose presence/absence the test actually cares about, rather than exact sizes.
        assertThat( clo.findTerm( "connectivity", 500 ) )
                .isEmpty();
        // "connective" is excluded from stemming, so it matches only the connective-tissue terms.
        assertThat( clo.findTerm( "connective", 500 ) )
                .hasSizeGreaterThanOrEqualTo( 10 )
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "dense regular connective tissue", "dense irregular connective tissue" );
        // "connect" is stemmed, so it matches terms via their labels/definitions/synonyms (e.g. "anatomical
        // structure"), but must NOT pull in the connective-tissue terms, which only match the un-stemmed
        // "connective" (see excluded words above).
        assertThat( clo.findTerm( "connect", 500 ) )
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "anatomical structure", "anatomical system" )
                .doesNotContain( "dense regular connective tissue", "dense irregular connective tissue" );
        assertThat( clo.findTerm( "connection", 500 ) )
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "anatomical structure", "anatomical system" )
                .doesNotContain( "dense regular connective tissue", "dense irregular connective tissue" );
    }
}