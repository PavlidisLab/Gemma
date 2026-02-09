package ubic.gemma.core.ontology;

import org.junit.Test;

import static org.junit.Assert.*;
import static ubic.gemma.core.ontology.OntologyUtils.termIdToUri;
import static ubic.gemma.core.ontology.OntologyUtils.uriToTermId;

public class OntologyUtilsTest {

    @Test
    public void isTermId() {
        assertTrue( OntologyUtils.isTermId( "FOO:123" ) );
        assertFalse( OntologyUtils.isTermId( "FOO:123", true ) );
        assertTrue( OntologyUtils.isTermId( "UBERON:123", false ) );
        assertTrue( OntologyUtils.isTermId( "uberon:123", true ) );
        assertTrue( OntologyUtils.isTermId( "TGEMO:123", false ) );
        assertTrue( OntologyUtils.isTermId( "TGEMO:123", true ) );
        assertFalse( OntologyUtils.isTermId( "FOO:123 " ) );
        assertFalse( OntologyUtils.isTermId( "FOO:123b" ) );
        assertFalse( OntologyUtils.isTermId( "FOO1:123" ) );
    }

    @Test
    public void testTermIdToUri() {
        assertEquals( "http://purl.obolibrary.org/obo/UBERON_000001", termIdToUri( "UBERON:000001" ) );
        assertEquals( "http://gemma.msl.ubc.ca/ont/TGEMO_000001", termIdToUri( "TGEMO:000001" ) );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_000001", termIdToUri( "EFO:000001" ) );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_000001", termIdToUri( "efo:000001" ) );
        assertEquals( "http://purl.obolibrary.org/obo/NCBITaxon_9606", termIdToUri( "NCBITaxon:9606" ) );
        assertEquals( "http://purl.obolibrary.org/obo/NCBITaxon_9606", termIdToUri( "ncbitaxon:9606" ) );
    }

    @Test
    public void testUriToTermId() {
        assertEquals( "UBERON:000001", uriToTermId( "http://purl.obolibrary.org/obo/UBERON_000001" ) );
        assertEquals( "TGEMO:000001", uriToTermId( "http://gemma.msl.ubc.ca/ont/TGEMO_000001" ) );
        assertEquals( "EFO:000001", uriToTermId( "http://www.ebi.ac.uk/efo/EFO_000001" ) );
        assertEquals( "NCBITaxon:9606", uriToTermId( "http://purl.obolibrary.org/obo/NCBITaxon_9606" ) );
        assertEquals( "tgemo:000001", uriToTermId( "http://gemma.msl.ubc.ca/ont/tgemo_000001" ) );
    }
}