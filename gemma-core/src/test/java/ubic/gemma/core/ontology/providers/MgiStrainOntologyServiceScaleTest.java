package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.search.OntologySearchResult;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Full-scale sanity check against a real MGI_Strain.rpt (~118k strains). Skipped unless
 * {@code -Dmgi.strain.rpt=/path/to/MGI_Strain.rpt} is supplied. Run with:
 * {@code mvn -pl gemma-core -DexcludedGroups= -Dtest=MgiStrainOntologyServiceScaleTest -Dmgi.strain.rpt=... test}
 */
@Tag("slow")
class MgiStrainOntologyServiceScaleTest {

    @Test
    void loadsAndSearchesRealMgiStrains() throws Exception {
        String p = System.getProperty( "mgi.strain.rpt" );
        assumeTrue( p != null && Files.isReadable( Path.of( p ) ), "set -Dmgi.strain.rpt to a real MGI_Strain.rpt" );

        MgiStrainOntologyService s = new MgiStrainOntologyService();
        Runtime rt = Runtime.getRuntime();
        long t0 = System.nanoTime();
        try ( FileInputStream is = new FileInputStream( p ) ) {
            s.initialize( is, true );
        }
        long ms = ( System.nanoTime() - t0 ) / 1_000_000;
        System.gc();
        long heapMb = ( rt.totalMemory() - rt.freeMemory() ) / ( 1024 * 1024 );
        int n = s.getAllURIs().size();
        System.out.printf( "MGI strains: loaded %,d in %,d ms; retained heap ~%,d MB%n", n, ms, heapMb );

        assertTrue( s.isOntologyLoaded() );
        assertTrue( n > 50_000, "expected >50k strains, got " + n );

        // exact common-background match must rank first despite thousands of C57BL/6-derived congenics
        Collection<OntologySearchResult<OntologyTerm>> hits = s.findTerm( "C57BL/6J", 5000 );
        assertFalse( hits.isEmpty() );
        assertEquals( "C57BL/6J", hits.iterator().next().getResult().getLabel(),
                "exact strain C57BL/6J must rank first" );
    }
}
