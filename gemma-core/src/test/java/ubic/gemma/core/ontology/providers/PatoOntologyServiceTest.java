package ubic.gemma.core.ontology.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.util.test.category.SlowTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@CommonsLog
public class PatoOntologyServiceTest {

    @Test
    @Category(SlowTest.class)
    public void test() throws OntologySearchException {
        PatoOntologyService pato = new PatoOntologyService();
        pato.initialize( true, true );
        assertTrue( pato.isOntologyLoaded() );
        assertFalse( pato.findTerm( "left" ).isEmpty() );
    }
}