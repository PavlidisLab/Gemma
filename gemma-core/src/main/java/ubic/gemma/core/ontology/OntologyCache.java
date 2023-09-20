package ubic.gemma.core.ontology;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.math3.util.Combinations;
import org.springframework.cache.Cache;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.persistence.util.CacheUtils;

import javax.annotation.Nullable;
import java.util.*;

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
        Cache.ValueWrapper value = cache.get( key );
        if ( value != null ) {
            //noinspection unchecked
            return ( Set<OntologyTerm> ) value.get();
        } else {
            if ( termsSet.size() > 1 ) {
                //noinspection unchecked
                HashSet<ParentsOrChildrenCacheKey> keys = new HashSet<>( ( Collection<ParentsOrChildrenCacheKey> ) CacheUtils.getKeys( cache ) );

                // try looking for k-1 or k-2 subsets
                ParentsOrChildrenCacheKey keyForSubset = lookupMaximalSubsetByCombination( keys, os, termsSet, direct, includeAdditionalProperties );

                // try enumerating keys (initially fast, but gets slower as the cache grows)
                if ( keyForSubset == null ) {
                    keyForSubset = lookupMaximalSubsetByEnumeratingKeys( keys, os, termsSet, direct, includeAdditionalProperties );
                }

                if ( keyForSubset != null ) {
                    Cache.ValueWrapper valueForSubset = cache.get( keyForSubset );
                    if ( valueForSubset != null ) {
                        //noinspection unchecked
                        Set<OntologyTerm> resultsForSubset = ( Set<OntologyTerm> ) valueForSubset.get();
                        // only query the difference
                        Set<OntologyTerm> remainingTerms = new HashSet<>( termsSet );
                        remainingTerms.removeAll( keyForSubset.terms );
                        Set<OntologyTerm> remainingResults = getParentsOrChildren( os, remainingTerms, direct, includeAdditionalProperties, cache, ancestors );
                        // recombine the results
                        Set<OntologyTerm> results = new HashSet<>( resultsForSubset );
                        results.addAll( remainingResults );
                        cache.put( key, results );
                        return results;
                    }
                }
            }

            // no subsets are of any use, so directly query
            try ( CacheUtils.Lock ignored = CacheUtils.acquireWriteLock( cache, key ) ) {
                // check if the entry have been computed by another thread
                value = cache.get( key );
                if ( value != null ) {
                    //noinspection unchecked
                    return ( Set<OntologyTerm> ) value.get();
                }
                Set<OntologyTerm> newVal = ancestors ?
                        os.getParents( termsSet, direct, includeAdditionalProperties ) :
                        os.getChildren( termsSet, direct, includeAdditionalProperties );
                cache.put( key, newVal );
                return newVal;
            }
        }
    }

    /**
     * A HashSet implementation with a cheap hashCode() operation.
     */
    private static class IncrementalHashSet<T> extends HashSet<T> {

        private int hashCode = 0;

        public IncrementalHashSet( Set<T> terms ) {
            super( terms );
        }

        @Override
        public boolean add( T o ) {
            if ( !super.add( o ) ) {
                hashCode += o.hashCode();
                return true;
            }
            return false;
        }

        @Override
        public boolean remove( Object o ) {
            if ( !super.remove( o ) ) {
                hashCode -= o.hashCode();
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Check if a k-1 (or k-2) subset of a given set of terms is in the given cache and query the difference.
     * <p>
     * Because the number of subset is exponential in the number of terms, we only try subsets of size 1 and 2 if
     * {@code n < 100}.
     */
    @Nullable
    private ParentsOrChildrenCacheKey lookupMaximalSubsetByCombination( Set<ParentsOrChildrenCacheKey> keys, OntologyService os, Set<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        // we will be generating subsets from this
        List<OntologyTerm> orderedTerms = new ArrayList<>( terms );
        // we will be mutating this
        Set<OntologyTerm> termsForSubset = new IncrementalHashSet<>( terms );
        // successively try removing k-subsets (k = 1 up to 3); it grows exponentially so careful here!
        int n = orderedTerms.size();
        // n = 100 has ~5000 2-combinations
        int maxN = n < 100 ? 2 : 1;
        // if n = k, there's only one subset, and it's the same case as if no subsets were found
        for ( int k = 1; k <= Math.min( n - 1, maxN ); k++ ) {
            for ( int[] is : new Combinations( n, k ) ) {
                for ( int i : is ) {
                    termsForSubset.remove( orderedTerms.get( i ) );
                }
                // note: ParentsOrChildrenCacheKey is immutable so that the hashCode can be efficiently computed
                ParentsOrChildrenCacheKey keyForSubset = new ParentsOrChildrenCacheKey( os, termsForSubset, direct, includeAdditionalProperties );
                if ( keys.contains( keyForSubset ) ) {
                    return keyForSubset;
                }
                for ( int i : is ) {
                    termsForSubset.add( orderedTerms.get( i ) );
                }
            }
        }
        return null;
    }

    /**
     * Enumerate the cache's keys to find the maximal subset.
     * <p>
     * This is less efficient than {@link #lookupMaximalSubsetByCombination(Set, OntologyService, Set, boolean, boolean)}
     * because we to verify if a subset exist for each key of the cache.
     */
    @Nullable
    private ParentsOrChildrenCacheKey lookupMaximalSubsetByEnumeratingKeys( Collection<ParentsOrChildrenCacheKey> keys, OntologyService os, Set<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return keys.stream()
                .filter( k -> k.ontologyService.equals( os ) && k.direct == direct && k.includeAdditionalProperties == includeAdditionalProperties && terms.containsAll( k.terms ) )
                .max( Comparator.comparingInt( k1 -> k1.terms.size() ) )
                .orElse( null );
    }

    @Value
    @EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
    private static class ParentsOrChildrenCacheKey {
        ubic.basecode.ontology.providers.OntologyService ontologyService;
        Set<OntologyTerm> terms;
        boolean direct;
        boolean includeAdditionalProperties;
    }
}
