package ubic.gemma.core.ontology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.providers.OntologyService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class OntologyCacheTest {

    private OntologyService ontologyService;
    private OntologyCache ontologyCache;
    private OntologyTerm term1, term2, term3, term4;

    @Before
    public void setUp() {
        ontologyService = mock( OntologyService.class );
        ontologyCache = new OntologyCache( new ConcurrentMapCache( "search" ), new ConcurrentMapCache( "parents" ), new ConcurrentMapCache( "children" ) );
        term1 = new OntologyTermSimple( "http://example.com/term1", "term1" );
        term2 = new OntologyTermSimple( "http://example.com/term2", "term2" );
        term3 = new OntologyTermSimple( "http://example.com/term3", "term3" );
        term4 = new OntologyTermSimple( "http://example.com/term3", "term4" );
    }

    @After
    public void resetMocks() {
        reset( ontologyService );
    }

    @Test
    public void testLookupByMaximalSubset() throws InterruptedException {
        ontologyCache.getChildren( ontologyService, Collections.singleton( term1 ), true, true );
        verify( ontologyService ).getChildren( Collections.singleton( term1 ), true, true );

        // a k-2 subset is cached (i.e. [term1]), so only the difference has to be queried
        Set<OntologyTerm> ret1 = ontologyCache.getChildren( ontologyService, Arrays.asList( term1, term2, term3 ), true, true );
        verify( ontologyService, atMostOnce() ).getChildren( Collections.singleton( term1 ), true, true );
        verify( ontologyService, atMostOnce() ).getChildren( new HashSet<>( Arrays.asList( term2, term3 ) ), true, true );
        verify( ontologyService, never() ).getChildren( new HashSet<>( Arrays.asList( term1, term2, term3 ) ), true, true );

        // a cached result!
        Set<OntologyTerm> ret2 = ontologyCache.getChildren( ontologyService, Arrays.asList( term1, term2, term3 ), true, true );
        assertSame( ret1, ret2 );

        // term1 is already in the cache, so only term2 is queried
        ontologyCache.getChildren( ontologyService, Arrays.asList( term1, term2 ), true, true );
        verify( ontologyService, atMostOnce() ).getChildren( Collections.singleton( term1 ), true, true );
        verify( ontologyService ).getChildren( Collections.singleton( term2 ), true, true );
        verify( ontologyService, never() ).getChildren( new HashSet<>( Arrays.asList( term1, term2 ) ), true, true );
    }

    @Test
    public void testLookupByMaximalSubsetWhenMinSubsetSizeIsSet() throws InterruptedException {
        ontologyCache.getChildren( ontologyService, Collections.singleton( term1 ), true, true );
        verify( ontologyService ).getChildren( Collections.singleton( term1 ), true, true );

        ontologyCache.setMinSubsetSize( 2 );

        // a subset of size 1 exists, but it cannot be used
        ontologyCache.getChildren( ontologyService, Arrays.asList( term1, term2, term3, term4 ), true, true );
        verify( ontologyService ).getChildren( new HashSet<>( Arrays.asList( term1, term2, term3, term4 ) ), true, true );
    }
}