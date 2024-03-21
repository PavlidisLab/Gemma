package ubic.gemma.core.ontology;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.cache.Cache;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.persistence.util.CacheUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * High-level cache abstraction for retrieving parents and children of a set of terms.
 * <p>
 * The main approach here for caching is to enumerate cache keys to find subsets of a given query and only retrieve the
 * difference from the {@link OntologyService}.
 * @author poirigui
 */
@CommonsLog
class OntologyCache {

    private final Cache searchCache, parentsCache, childrenCache;

    private int minSubsetSize = 1;

    OntologyCache( Cache searchCache, Cache parentsCache, Cache childrenCache ) {
        this.searchCache = searchCache;
        this.parentsCache = parentsCache;
        this.childrenCache = childrenCache;
    }

    /**
     * Minimum size of subsets to consider when enumerating cache keys.
     */
    void setMinSubsetSize( int minSubsetSize ) {
        Assert.isTrue( minSubsetSize > 0 );
        this.minSubsetSize = minSubsetSize;
    }

    public Collection<OntologyTerm> findTerm( OntologyService ontology, String query ) throws OntologySearchException {
        SearchCacheKey key = new SearchCacheKey( ontology, query );

        try ( CacheUtils.Lock ignored = CacheUtils.acquireReadLock( searchCache, key ) ) {
            Cache.ValueWrapper value = searchCache.get( key );
            if ( value != null ) {
                //noinspection unchecked
                return ( Collection<OntologyTerm> ) value.get();
            }
        }

        try ( CacheUtils.Lock ignored = CacheUtils.acquireWriteLock( searchCache, key ) ) {
            Collection<OntologyTerm> results = ontology.findTerm( query );
            searchCache.put( key, results );
            return results;
        }
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
     * Clear the search  cache for all entries related to a given ontology service.
     * @param serv
     */
    public void clearSearchCacheByOntology( OntologyService serv ) {
        CacheUtils.evictIf( searchCache, key -> ( ( SearchCacheKey ) key ).getOntologyService().equals( serv ) );
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
        StopWatch timer = StopWatch.createStarted();

        Set<OntologyTerm> termsSet = new HashSet<>( terms );
        ParentsOrChildrenCacheKey key = new ParentsOrChildrenCacheKey( os, termsSet, direct, includeAdditionalProperties );

        // there might be a thread computing this cache entry
        long initialLockAcquisitionMs = timer.getTime();
        try ( CacheUtils.Lock ignored = CacheUtils.acquireReadLock( cache, key ) ) {
            initialLockAcquisitionMs = timer.getTime() - initialLockAcquisitionMs;
            Cache.ValueWrapper value = cache.get( key );
            if ( value != null ) {
                //noinspection unchecked
                return ( Set<OntologyTerm> ) value.get();
            }
        }

        long lookupSubsetMs = 0;
        ParentsOrChildrenCacheKey keyForSubset;
        // enough terms to make it worth looking for subsets...
        if ( termsSet.size() >= minSubsetSize + 1 ) {
            lookupSubsetMs = timer.getTime();
            keyForSubset = lookupMaximalSubsetByEnumeratingKeys( cache, os, termsSet, direct, includeAdditionalProperties );
            lookupSubsetMs = timer.getTime() - lookupSubsetMs;
            if ( lookupSubsetMs > 100 ) {
                log.warn( String.format( "Enumerating cache keys for finding a maximal subset for %s of %s took %d ms and %s",
                        ancestors ? "parents" : "children", key, lookupSubsetMs, keyForSubset != null ? "succeeded with " + keyForSubset + " terms" : "failed" ) );
            }
        } else {
            // we used to enumerate all possible k-1, k-2 subsets, but that's just too slow compared to enumerating
            // cache keys, other strategies can be implemented here if necessary
            keyForSubset = null;
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
            } else {
                log.warn( "Missing expected key from the " + ( ancestors ? "parents" : "children" ) + " cache: " + keyForSubset );
            }
        }

        long acquireMs = timer.getTime();
        long computingMs = 0;
        try ( CacheUtils.Lock ignored = CacheUtils.acquireWriteLock( cache, key ) ) {
            acquireMs = timer.getTime() - acquireMs;
            // lookup the cache in case another thread computed the result while we were enumerating subsets
            Cache.ValueWrapper value = cache.get( key );
            if ( value != null ) {
                //noinspection unchecked
                return ( Set<OntologyTerm> ) value.get();
            }
            computingMs = timer.getTime();
            // no subset found in the cache, just compute it from scratch
            Set<OntologyTerm> newVal = ancestors ?
                    os.getParents( termsSet, direct, includeAdditionalProperties ) :
                    os.getChildren( termsSet, direct, includeAdditionalProperties );
            computingMs = timer.getTime() - computingMs;
            // ignore empty newVal, it might just be that the ontology is not initialized yet
            if ( !newVal.isEmpty() && computingMs < lookupSubsetMs ) {
                log.warn( String.format( "Computing %d %s terms for %s took less time than looking up subsets, increasing the minSubsetSize might be beneficial",
                        newVal.size(),
                        ancestors ? "parents" : "children",
                        key ) );
            }
            cache.put( key, newVal );
            return newVal;
        } finally {
            if ( timer.getTime() > 500 ) {
                log.warn( String.format( "Retrieving %s for %s took %d ms (acquiring locks: %d ms, enumerating subsets: %d ms, computing: %d ms)",
                        ancestors ? "parents" : "children", key, timer.getTime(), initialLockAcquisitionMs + acquireMs, lookupSubsetMs, computingMs ) );
            }
        }
    }

    /**
     * Enumerate the cache's keys to find the maximal subset.
     */
    @Nullable
    private ParentsOrChildrenCacheKey lookupMaximalSubsetByEnumeratingKeys( Cache cache, OntologyService os, Set<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return CacheUtils.getKeys( cache ).stream()
                .map( o -> ( ParentsOrChildrenCacheKey ) o )
                .filter( k -> k.direct == direct && k.includeAdditionalProperties == includeAdditionalProperties && k.ontologyService.equals( os ) )
                // ignore empty subsets, those will cause an infinite loop
                // skip sets which are larger or equal in size, those cannot be subsets
                .filter( k -> k.terms.size() >= minSubsetSize && k.terms.size() < terms.size() && terms.containsAll( k.terms ) )
                .max( Comparator.comparingInt( k1 -> k1.terms.size() ) )
                .orElse( null );
    }

    @Value
    @EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
    private static class ParentsOrChildrenCacheKey {
        OntologyService ontologyService;
        Set<OntologyTerm> terms;
        boolean direct;
        boolean includeAdditionalProperties;

        @Override
        public String toString() {
            return String.format( "%d terms from %s [%s] [%s]", terms.size(), ontologyService,
                    direct ? "direct" : "all",
                    includeAdditionalProperties ? "subClassOf and " + ontologyService.getAdditionalPropertyUris().size() + " additional properties" : "only subClassOf" );
        }
    }

    @Value
    private static class SearchCacheKey {
        OntologyService ontologyService;
        String query;
    }
}
