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

class MgiStrainOntologyServiceTest {

    /** A tiny MGI_Strain.rpt: tab-delimited MGI:id / nomenclature / type, plus a comment and junk lines. */
    private static final String RPT = String.join( "\n",
            "# MGI Strain report",
            "MGI:2160170\t101\tinbred strain",
            "MGI:3028467\tC57BL/6J\tinbred strain",
            "MGI:2159737\tBALB/cJ\tinbred strain",
            "MGI:7571118\t101;C3HCat2<ns>/Hmgu\tNot Applicable",
            "MGI:9999999\t\tNot Specified",           // no name -> skipped
            "notMgi\tSomething\tinbred strain",         // no MGI: accession -> skipped
            "" );

    private static final String B6J = "https://www.informatics.jax.org/strain/MGI:3028467";
    private static final String NONAME = "https://www.informatics.jax.org/strain/MGI:9999999";

    private MgiStrainOntologyService load() {
        MgiStrainOntologyService s = new MgiStrainOntologyService();
        s.initialize( new ByteArrayInputStream( RPT.getBytes( StandardCharsets.UTF_8 ) ), true );
        return s;
    }

    @Test
    void parsesStrainsSkippingCommentsAndBlanks() {
        MgiStrainOntologyService s = load();
        assertTrue( s.isOntologyLoaded() );
        assertEquals( 4, s.getAllURIs().size() );
        assertTrue( s.getAllURIs().contains( B6J ) );
        assertFalse( s.getAllURIs().contains( NONAME ) );
    }

    @Test
    void getTermResolvesCanonicalUri() {
        MgiStrainOntologyService s = load();
        OntologyTerm b6 = s.getTerm( B6J );
        assertNotNull( b6 );
        assertEquals( "C57BL/6J", b6.getLabel() );
        assertNull( s.getTerm( NONAME ) );
    }

    @Test
    void findTermRanksExactStrainFirst() throws Exception {
        MgiStrainOntologyService s = load();
        Collection<OntologySearchResult<OntologyTerm>> hits = s.findTerm( "C57BL/6J", 10 );
        assertFalse( hits.isEmpty() );
        assertEquals( B6J, hits.iterator().next().getResult().getUri(),
                "exact strain match should rank first" );
        assertTrue( s.findTerm( "BALB/cJ", 10 ).stream()
                .anyMatch( r -> "BALB/cJ".equals( r.getResult().getLabel() ) ) );
    }
}
