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
        assertThat( clo.findTerm( "connectivity", 500 ) )
                .isEmpty();
        assertThat( clo.findTerm( "connective", 500 ) )
                .hasSize( 21 )
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "dense regular connective tissue", "dense irregular connective tissue" );
        assertThat( clo.findTerm( "connect", 500 ) )
                .hasSize( 10 )
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .contains( "connects", "proximally connected to" )
                .doesNotContain( "dense regular connective tissue", "dense irregular connective tissue" );
        assertThat( clo.findTerm( "connection", 500 ) )
                .hasSize( 10 )
                .extracting( OntologySearchResult::getResult )
                .extracting( OntologyTerm::getLabel )
                .doesNotContain( "dense regular connective tissue", "dense irregular connective tissue" );
    }
}
