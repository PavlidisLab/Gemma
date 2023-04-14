package ubic.gemma.core.ontology.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import ubic.basecode.ontology.search.OntologySearchException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@CommonsLog
public class PatoOntologyServiceTest {

    @Test
    public void test() throws OntologySearchException {
        PatoOntologyService pato = new PatoOntologyService();
        pato.initialize( true, true );
        assertTrue( pato.isOntologyLoaded() );
        assertFalse( pato.findTerm( "left" ).isEmpty() );
    }
}