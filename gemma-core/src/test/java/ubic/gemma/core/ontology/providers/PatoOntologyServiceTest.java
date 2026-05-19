package ubic.gemma.core.ontology.providers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;
import ubic.gemma.core.util.test.category.SlowTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public class PatoOntologyServiceTest {

    @Test
    @Category(SlowTest.class)
    public void test() throws OntologySearchException {
        PatoOntologyService pato = new PatoOntologyService();
        pato.initialize( true, true );
        assertTrue( pato.isOntologyLoaded() );
        assertFalse( pato.findTerm( "left", 10 ).isEmpty() );
    }
}