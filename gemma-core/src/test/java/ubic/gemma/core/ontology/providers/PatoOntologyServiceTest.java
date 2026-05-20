package ubic.gemma.core.ontology.providers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class PatoOntologyServiceTest {

    @Test
    @Tag("slow")
    public void test() throws OntologySearchException {
        PatoOntologyService pato = new PatoOntologyService();
        pato.initialize( true, true );
        assertTrue( pato.isOntologyLoaded() );
        assertFalse( pato.findTerm( "left", 10 ).isEmpty() );
    }
}