package ubic.gemma.core.ontology.providers;

import org.junit.Test;

import static org.junit.Assert.*;
import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;
import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.isGoId;

public class GeneOntologyUtilsTest {

    @Test
    public void testIsGoId() {
        assertTrue( isGoId( "GO:11209200" ) );
        assertTrue( isGoId( "GO_11209200" ) );
        assertTrue( isGoId( "http://purl.obolibrary.org/obo/GO_11209200" ) );
        assertFalse( isGoId( "http://purl.obolibrary.org/obo/MONDO_11209200" ) );
    }

    @Test
    public void testAsRegularGoId() {
        assertEquals( "GO:0000001", asRegularGoId( "GO:0000001" ) );
        assertEquals( "GO:0000001", asRegularGoId( "GO_0000001" ) );
        assertEquals( "GO:0000001", asRegularGoId( "http://purl.obolibrary.org/obo/GO_0000001" ) );
        assertNull( asRegularGoId( "MONDO:0000001" ) );
    }
}