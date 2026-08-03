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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Full-scale sanity check against a real Cellosaurus OBO (~169k terms, ~113 MB). Skipped unless
 * {@code -Dcellosaurus.obo=/path/to/cellosaurus.obo} is supplied, so it never needs a committed
 * 113 MB fixture or network. Run with:
 * {@code mvn -pl gemma-core -DexcludedGroups= -Dtest=CellosaurusOntologyServiceScaleTest -Dcellosaurus.obo=... -DargLine=-Xmx4g test}
 */
@Tag("slow")
class CellosaurusOntologyServiceScaleTest {

    @Test
    void loadsAndSearchesRealCellosaurus() throws Exception {
        String p = System.getProperty( "cellosaurus.obo" );
        assumeTrue( p != null && Files.isReadable( Path.of( p ) ), "set -Dcellosaurus.obo to a real cellosaurus.obo" );

        CellosaurusOntologyService s = new CellosaurusOntologyService();

        Runtime rt = Runtime.getRuntime();
        long t0 = System.nanoTime();
        try ( FileInputStream is = new FileInputStream( p ) ) {
            s.initialize( is, true );
        }
        long ms = ( System.nanoTime() - t0 ) / 1_000_000;
        System.gc();
        long heapMb = ( rt.totalMemory() - rt.freeMemory() ) / ( 1024 * 1024 );
        int n = s.getAllURIs().size();
        System.out.printf( "Cellosaurus: loaded %,d terms in %,d ms; retained heap ~%,d MB; version=%s%n",
                n, ms, heapMb, s.getVersion() );

        assertTrue( s.isOntologyLoaded() );
        assertTrue( n > 100_000, "expected >100k terms, got " + n );

        // canonical HeLa CVCL_0030 must resolve and be findable
        OntologyTerm hela = s.getTerm( "https://www.cellosaurus.org/CVCL_0030" );
        assertNotNull( hela, "HeLa CVCL_0030 should resolve" );

        // The exact-name match must rank FIRST despite hundreds of "HeLa *" derivatives (exact-boost).
        String heLaUri = "https://www.cellosaurus.org/CVCL_0030";
        java.util.List<OntologySearchResult<OntologyTerm>> hits = new java.util.ArrayList<>( s.findTerm( "HeLa", 5000 ) );
        int rank = -1;
        for ( int i = 0; i < hits.size(); i++ ) {
            if ( heLaUri.equals( hits.get( i ).getResult().getUri() ) ) { rank = i; break; }
        }
        System.out.printf( "HeLa: %,d hits total; CVCL_0030 rank=%d%n", hits.size(), rank );
        assertEquals( 0, rank, "exact match HeLa (CVCL_0030) must rank first, not be buried under derivatives" );

        assertTrue( s.findTerm( "KOLF2.1J", 25 ).stream().findAny().isPresent(),
                "findTerm(KOLF2.1J) should return something" );
    }

    private static boolean containsUri( Collection<OntologySearchResult<OntologyTerm>> results, String uri ) {
        return results.stream().anyMatch( r -> uri.equals( r.getResult().getUri() ) );
    }
}
