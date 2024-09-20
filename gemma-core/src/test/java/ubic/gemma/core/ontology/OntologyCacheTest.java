package ubic.gemma.core.ontology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
        term4 = new OntologyTermSimple( "http://example.com/term4", "term4" );
    }

    @After
    public void resetMocks() {
        reset( ontologyService );
    }

    @Test
    public void testFindTerms() throws OntologySearchException {
        OntologyService os = mock();
        when( os.findTerm( "test", 10 ) )
                .thenReturn( Arrays.asList( new OntologySearchResult<>( term1, 1 ), new OntologySearchResult<>( term2, 10 ), new OntologySearchResult<>( term3, 15 ) ) );
        ontologyCache.findTerm( os, "test", 10 );
        ontologyCache.findTerm( os, "test", 5 ); // this query will be cached
        ontologyCache.findTerm( os, "test", 15 ); // this query will be cached as well becaue we returned less than 10 terms
        assertThat( ontologyCache.findTerm( os, "test", 2 ) )
                .extracting( OntologySearchResult::getResult )
                .containsExactlyInAnyOrder( term2, term3 );
        verify( os ).findTerm( "test", 10 );
        verifyNoMoreInteractions( os );
    }

    @Test
    public void testLookupByMaximalSubset() {
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
    public void testLookupByMaximalSubsetWhenMinSubsetSizeIsSet() {
        ontologyCache.getChildren( ontologyService, Collections.singleton( term1 ), true, true );
        verify( ontologyService ).getChildren( Collections.singleton( term1 ), true, true );

        ontologyCache.setMinSubsetSize( 2 );

        // a subset of size 1 exists, but it cannot be used
        ontologyCache.getChildren( ontologyService, Arrays.asList( term1, term2, term3, term4 ), true, true );
        verify( ontologyService ).getChildren( Arrays.asList( term1, term2, term3, term4 ), true, true );
    }
}