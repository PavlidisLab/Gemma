package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.search.OntologySearchResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MgiAlleleOntologyServiceTest {

    /**
     * A tiny MGI_PhenotypicAllele.rpt: 13 tab-delimited columns (accession, symbol, name, type, attribute, PubMed,
     * gene accession, gene symbol, RefSeq, Ensembl, MP ids, synonyms, gene name). The Smn1 row is copied from the
     * 2026-09-14 report; the transgene has no gene.
     */
    private static final String RPT = String.join( "\n",
            "# MGI phenotypic allele report",
            "MGI:3794202\tSmn1<tm5(Smn1/SMN2)Mrph>\ttargeted mutation 5, Andrew Murphy\tTargeted\tHumanized sequence"
                    + "\t\tMGI:98357\tSmn1\tNM_011420\tENSMUSG00000021645\tMP:0005386\tSmn allele C\tsurvival motor neuron 1",
            "MGI:3524957\tTg(APPswe,PSEN1dE9)85Dbo\ttransgene insertion 85, David R Borchelt\tTransgenic\tInserted expressed sequence"
                    + "\t\t\t\t\t\tMP:0003635\t\t",
            "MGI:9999999\t\tno symbol\tTargeted\t\t\t\t\t\t\t\t\t",          // no symbol -> skipped
            "notMgi\tXyz<tm1>\tname\tTargeted\t\t\t\t\t\t\t\t\t",           // no MGI: accession -> skipped
            "" );

    private static final String SMN1_C = "https://www.informatics.jax.org/allele/MGI:3794202";

    private MgiAlleleOntologyService load() {
        MgiAlleleOntologyService s = new MgiAlleleOntologyService();
        s.initialize( new ByteArrayInputStream( RPT.getBytes( StandardCharsets.UTF_8 ) ), true );
        return s;
    }

    @Test
    void parsesAllelesSkippingCommentsAndIncompleteRows() {
        MgiAlleleOntologyService s = load();
        assertTrue( s.isOntologyLoaded() );
        assertEquals( 2, s.getAllURIs().size() );
        assertTrue( s.getAllURIs().contains( SMN1_C ) );
        assertTrue( s.getAllURIs().contains( "https://www.informatics.jax.org/allele/MGI:3524957" ) );
        assertNull( s.getTerm( "https://www.informatics.jax.org/allele/MGI:9999999" ) );
    }

    /** The URI has to be the one the MGI relations are stored under, or annotating with it reaches nothing. */
    @Test
    void theTermUriIsTheSubjectUriOfMgisRelations() {
        OntologyTerm t = load().getTerm( SMN1_C );
        assertNotNull( t );
        assertEquals( "Smn1<tm5(Smn1/SMN2)Mrph>", t.getLabel() );
    }

    /** GSE322566's paper calls it "the C/C mouse"; MGI's synonym column is where "Smn allele C" lives. */
    @Test
    void findTermMatchesMgisSynonym() throws Exception {
        Collection<OntologySearchResult<OntologyTerm>> hits = load().findTerm( "Smn allele C", 10 );
        assertFalse( hits.isEmpty() );
        assertEquals( SMN1_C, hits.iterator().next().getResult().getUri() );
    }

    @Test
    void isSupplementary() {
        assertTrue( new MgiAlleleOntologyService().isSupplementary() );
    }
}
