package ubic.gemma.core.ontology;

import lombok.Value;
import org.apache.commons.math3.util.Combinations;
import org.springframework.cache.Cache;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.persistence.util.CacheUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-level cache abstraction for retrieving parents and children of a set of terms.
 * <p>
 * The main approach here for caching is to lookup all the possible {@code k-1} subsets (then {@code k - 2},
 * {@code k - 3}, ...) of a given query and only retrieve the difference from the {@link OntologyService}.
 * @author poirigui
 */
class OntologyCache {

    private final Cache parentsCache, childrenCache;

    OntologyCache( Cache parentsCache, Cache childrenCache ) {
        this.parentsCache = parentsCache;
        this.childrenCache = childrenCache;
    }

    /**
     * Obtain the parents of a given set of terms.
     */
    Set<OntologyTerm> getParents( OntologyService os, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return getParentsOrChildren( os, terms, direct, includeAdditionalProperties, parentsCache, true );
    }

    /**
     * Obtain the children of a given set of terms.
     */
    Set<OntologyTerm> getChildren( OntologyService os, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return getParentsOrChildren( os, terms, direct, includeAdditionalProperties, childrenCache, false );
    }

    /**
     * Clear the cache for all entries related to a given ontology service.
     */
    void clearByOntology( OntologyService serv ) {
        CacheUtils.evictIf( parentsCache, key -> ( ( ParentsOrChildrenCacheKey ) key ).getOntologyService().equals( serv ) );
        CacheUtils.evictIf( childrenCache, key -> ( ( ParentsOrChildrenCacheKey ) key ).getOntologyService().equals( serv ) );
    }

    private Set<OntologyTerm> getParentsOrChildren( OntologyService os, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, Cache cache, boolean ancestors ) {
        if ( terms.isEmpty() ) {
            return Collections.emptySet();
        }
        Set<OntologyTerm> termsSet = new HashSet<>( terms );
        Object key = new ParentsOrChildrenCacheKey( os, termsSet, direct, includeAdditionalProperties );
        // The write lock prevents a scenario where multiple threads attempt to produce the same cache entry (common
        // in GemBrow since it issues multiple queries with the same terms). Only the first thread will generate the
        // entry and all the other will wait
        return CacheUtils.computeIfMissing( cache, key, () -> {
            Set<OntologyTerm> children;
            if ( termsSet.size() == 1 ) {
                // singleton set, no subset are of any use, so directly query
                children = ancestors ?
                        os.getParents( termsSet, direct, includeAdditionalProperties ) :
                        os.getChildren( termsSet, direct, includeAdditionalProperties );
            } else {
                children = lookupSubsets( os, termsSet, direct, includeAdditionalProperties, cache, ancestors );
            }
            return children;
        } );
    }

    /**
     * Check if a k-1 subset of a given set of terms is in the given cache and query the difference.
     * @return the children of the given terms
     */
    private Set<OntologyTerm> lookupSubsets( OntologyService os, Set<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, Cache cache, boolean ancestors ) {
        // we will be generating subsets from this
        List<OntologyTerm> orderedTerms = new ArrayList<>( terms );
        // we will be mutating this
        Set<OntologyTerm> remainingTerms = new HashSet<>();
        Set<OntologyTerm> termsForSubset = new HashSet<>( terms );
        ParentsOrChildrenCacheKey keyForSubset = new ParentsOrChildrenCacheKey( os, termsForSubset, direct, includeAdditionalProperties );
        // successively try removing k-subsets (k = 1 up to 3); it grows exponentially so careful here!
        int n = orderedTerms.size();
        // n = 100 has ~5000 2-combinations
        int maxN = n < 100 ? 2 : 1;
        // if n = k, there's only one subset, and it's the same case as if no subsets were found
        for ( int k = 1; k <= Math.min( n - 1, maxN ); k++ ) {
            for ( int[] is : new Combinations( n, k ) ) {
                for ( int i : is ) {
                    remainingTerms.add( orderedTerms.get( i ) );
                }
                termsForSubset.removeAll( remainingTerms );
                Cache.ValueWrapper valueForSubset = cache.get( keyForSubset );
                if ( valueForSubset != null ) {
                    //noinspection unchecked
                    Set<OntologyTerm> resultsForSubset = ( Set<OntologyTerm> ) valueForSubset.get();
                    Set<OntologyTerm> remainingResults = ancestors ?
                            os.getParents( remainingTerms, direct, includeAdditionalProperties )
                            : os.getChildren( remainingTerms, direct, includeAdditionalProperties );
                    Set<OntologyTerm> results = new HashSet<>( resultsForSubset );
                    results.addAll( remainingResults );
                    return results;
                }
                termsForSubset.addAll( remainingTerms );
                remainingTerms.clear();
            }
        }
        // no k-1 subset found
        return ancestors ?
                os.getParents( terms, direct, includeAdditionalProperties ) :
                os.getChildren( terms, direct, includeAdditionalProperties );
    }

    @Value
    private static class ParentsOrChildrenCacheKey {
        ubic.basecode.ontology.providers.OntologyService ontologyService;
        Set<OntologyTerm> terms;
        boolean direct;
        boolean includeAdditionalProperties;
    }
}
