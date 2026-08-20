package ubic.gemma.core.ontology;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
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
        // FIXME: this is not valid, but some ontologies use alphabetic character in the LOCALID part
        assertTrue( OntologyUtils.isTermId( "FOO:123b" ) );
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

    /**
     * Cellosaurus is off the PURL and on https, so it needs its own base in both directions.
     * {@code http://purl.obolibrary.org/obo/CVCL_1870} 404s — see {@code CellosaurusOntologyService}.
     */
    @Test
    public void testCellosaurusIsNotOnThePurl() {
        assertEquals( "https://www.cellosaurus.org/CVCL_1870", termIdToUri( "CVCL:1870" ) );
        assertEquals( "https://www.cellosaurus.org/CVCL_1870", termIdToUri( "cvcl:1870" ) );
        assertEquals( "CVCL:1870", uriToTermId( "https://www.cellosaurus.org/CVCL_1870" ) );
    }

    /**
     * The underscore spelling of an identifier, which is what a URI's tail and a term card both show.
     * A known ID space is the whole guard: without it this would swallow gene symbols.
     */
    @Test
    public void testLocalNameToTermId() {
        assertEquals( "CLO:0007606", OntologyUtils.localNameToTermId( "CLO_0007606" ) );
        assertEquals( "CVCL:1870", OntologyUtils.localNameToTermId( "CVCL_1870" ) );
        assertEquals( "EFO:0600015", OntologyUtils.localNameToTermId( "EFO_0600015" ) );
        assertEquals( "GO:0008150", OntologyUtils.localNameToTermId( "  GO_0008150  " ) );

        assertNull( OntologyUtils.localNameToTermId( "HLA_DRB1" ) );
        assertNull( OntologyUtils.localNameToTermId( "cell_type" ) );
        assertNull( OntologyUtils.localNameToTermId( "APP_PS1" ) );
        // the colon form is a different question and this one does not answer it
        assertNull( OntologyUtils.localNameToTermId( "GO:0008150" ) );
    }
}