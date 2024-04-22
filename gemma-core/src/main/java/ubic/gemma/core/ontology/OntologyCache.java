package ubic.gemma.core.ontology;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.cache.Cache;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.persistence.cache.CacheKeyLock;
import ubic.gemma.persistence.util.CacheUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    public OntologyCache( Cache searchCache, Cache parentsCache, Cache childrenCache ) {
        this.searchCache = searchCache;
        this.parentsCache = parentsCache;
        this.childrenCache = childrenCache;
    }

    /**
     * Minimum size of subsets to consider when enumerating cache keys.
     */
    public void setMinSubsetSize( int minSubsetSize ) {
        Assert.isTrue( minSubsetSize > 0 );
        this.minSubsetSize = minSubsetSize;
    }

    public Collection<OntologySearchResult<OntologyTerm>> findTerm( OntologyService ontology, String query, int maxResults ) throws OntologySearchException {
        SearchCacheKey key = new SearchCacheKey( ontology, query );

        try ( CacheKeyLock.LockAcquisition ignored = CacheUtils.acquireReadLock( searchCache, key ) ) {
            Cache.ValueWrapper value = searchCache.get( key );
            if ( value != null ) {
                //noinspection unchecked
                return ( Collection<OntologySearchResult<OntologyTerm>> ) value.get();
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            log.warn( "Current thread was interrupted while querying terms matching " + query + ", will return nothing instead.", e );
            return Collections.emptySet();
        }

        try ( CacheKeyLock.LockAcquisition ignored = CacheUtils.acquireWriteLock( searchCache, key ) ) {
            Collection<OntologySearchResult<OntologyTerm>> results = ontology.findTerm( query, maxResults );
            searchCache.put( key, results );
            return results;
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            log.warn( "Current thread was interrupted while querying terms matching " + query + ", will return nothing instead.", e );
            return Collections.emptySet();
        }
    }

    /**
     * Obtain the parents of a given set of terms.
     */
    public Set<OntologyTerm> getParents( OntologyService os, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return getParentsOrChildren( os, terms, direct, includeAdditionalProperties, parentsCache, true );
    }

    /**
     * Obtain the children of a given set of terms.
     */
    public Set<OntologyTerm> getChildren( OntologyService os, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return getParentsOrChildren( os, terms, direct, includeAdditionalProperties, childrenCache, false );
    }

    /**
     * Clear the search  cache for all entries related to a given ontology service.
     */
    public void clearSearchCacheByOntology( OntologyService serv ) {
        CacheUtils.evictIf( searchCache, key -> ( ( SearchCacheKey ) key ).getOntologyService().equals( serv ) );
    }

    /**
     * Clear the cache for all entries related to a given ontology service.
     */
    public void clearParentsAndChildrenCachesByOntology( OntologyService serv ) {
        CacheUtils.evictIf( parentsCache, key -> ( ( ParentsOrChildrenCacheKey ) key ).ontologyService.equals( serv ) );
        CacheUtils.evictIf( childrenCache, key -> ( ( ParentsOrChildrenCacheKey ) key ).ontologyService.equals( serv ) );
    }

    private Set<OntologyTerm> getParentsOrChildren( OntologyService os, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, Cache cache, boolean ancestors ) {
        if ( terms.isEmpty() ) {
            return Collections.emptySet();
        }
        StopWatch timer = StopWatch.createStarted();

        ParentsOrChildrenCacheKey key = new ParentsOrChildrenCacheKey( os, terms, direct, includeAdditionalProperties );

        String interruptedMessage = String.format( "Current thread was interrupted while retrieving %s for %s, will return nothing.", ancestors ? "parents" : "children", key );

        // there might be a thread computing this cache entry
        long initialLockAcquisitionMs = timer.getTime();
        try ( CacheKeyLock.LockAcquisition ignored = CacheUtils.acquireReadLock( cache, key ) ) {
            initialLockAcquisitionMs = timer.getTime() - initialLockAcquisitionMs;
            Cache.ValueWrapper value = cache.get( key );
            if ( value != null ) {
                if ( timer.getTime() > 500 ) {
                    log.warn( String.format( "Retrieving %s for %s took %d ms (acquiring read lock: %d ms)", ancestors ? "parents" : "children", key, timer.getTime(), initialLockAcquisitionMs ) );
                }
                //noinspection unchecked
                return ( Set<OntologyTerm> ) value.get();
            } else if ( initialLockAcquisitionMs > 100 ) {
                // this is a problem, if we've been waiting, there should be a value computed by another thread.
                log.warn( String.format( "Waited %d ms for a lock on %s[%s], but no value was found in the cache.", initialLockAcquisitionMs, cache, key ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            log.warn( interruptedMessage, e );
            return Collections.emptySet();
        }

        // first, check if there are subsets of the key in the cache, this can save substantial time, and it's very cheap to do
        long subsetLookupMs = timer.getTime();
        Set<OntologyTerm> keysForSubsets = new HashSet<>();
        Set<OntologyTerm> resultsForSubset = new HashSet<>();
        for ( ParentsOrChildrenCacheKey k : lookupSubsetsByEnumeratingKeys( cache, key ) ) {
            Cache.ValueWrapper v = cache.get( k );
            if ( v != null ) {
                keysForSubsets.addAll( k.terms );
                //noinspection unchecked
                resultsForSubset.addAll( ( Set<OntologyTerm> ) v.get() );
            } else {
                log.warn( "Missing expected key " + k + " from the " + ( ancestors ? "parents" : "children" ) + " cache." );
            }
        }
        subsetLookupMs = timer.getTime() - subsetLookupMs;

        long acquireMs = timer.getTime();
        long computingMs = 0L;
        try ( CacheKeyLock.LockAcquisition ignored = CacheUtils.acquireWriteLock( cache, key ) ) {
            acquireMs = timer.getTime() - acquireMs;
            Set<OntologyTerm> newVal;
            if ( cache.get( key ) != null ) {
                // another thread has computed the value in the meantime, it happens if multiple threads were trying to
                // acquire the write lock at the same time
                //noinspection unchecked
                newVal = ( Set<OntologyTerm> ) cache.get( key ).get();
            } else {
                computingMs = timer.getTime();
                if ( !keysForSubsets.isEmpty() ) {
                    // only query the difference
                    Set<OntologyTerm> remainingTerms = new HashSet<>( terms );
                    remainingTerms.removeAll( keysForSubsets );
                    Set<OntologyTerm> remainingResults = ancestors ? os.getParents( remainingTerms, direct, includeAdditionalProperties ) : os.getChildren( remainingTerms, direct, includeAdditionalProperties );
                    // recombine the results
                    newVal = new HashSet<>( resultsForSubset );
                    newVal.addAll( remainingResults );
                } else {
                    // no subset found in the cache, just compute it from scratch
                    newVal = ancestors ? os.getParents( terms, direct, includeAdditionalProperties ) : os.getChildren( terms, direct, includeAdditionalProperties );
                }
                computingMs = timer.getTime() - computingMs;
                cache.put( key, newVal );
            }
            if ( timer.getTime() > 500 ) {
                log.warn( String.format( "Retrieving %s for %s took %d ms (acquiring locks: %d ms, subset lookup: %d ms, computing: %d ms)", ancestors ? "parents" : "children", key, timer.getTime(), initialLockAcquisitionMs + acquireMs, subsetLookupMs, computingMs ) );
            }
            return newVal;
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            log.warn( interruptedMessage, e );
            return Collections.emptySet();
        }
    }

    /**
     * Lookup the cache for subsets of the given key.
     */
    private List<ParentsOrChildrenCacheKey> lookupSubsetsByEnumeratingKeys( Cache cache, ParentsOrChildrenCacheKey key ) {
        //noinspection unchecked
        return ( ( Collection<ParentsOrChildrenCacheKey> ) CacheUtils.getKeys( cache ) ).stream()
                // ignore empty subsets, those will cause an infinite loop
                .filter( k1 -> k1.terms.size() >= minSubsetSize ).filter( k1 -> k1.isSubsetOf( key ) ).collect( Collectors.toList() );
    }

    private static class ParentsOrChildrenCacheKey {
        private final OntologyService ontologyService;
        private final Set<OntologyTerm> terms;
        private final int termsHash;
        private final boolean direct;
        private final boolean includeAdditionalProperties;

        public ParentsOrChildrenCacheKey( OntologyService ontologyService, Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
            this.ontologyService = ontologyService;
            // baseCode implementation lookups labels, which is very inefficient
            this.terms = Collections.unmodifiableSet( new HashSet<>( terms ) );
            this.termsHash = this.terms.hashCode();
            this.direct = direct;
            this.includeAdditionalProperties = includeAdditionalProperties;
        }

        public boolean isSubsetOf( ParentsOrChildrenCacheKey other ) {
            return direct == other.direct && includeAdditionalProperties == other.includeAdditionalProperties && ontologyService.equals( other.ontologyService ) && terms.size() <= other.terms.size() && other.terms.containsAll( terms );
        }

        @Override
        public int hashCode() {
            return Objects.hash( ontologyService, termsHash, direct, includeAdditionalProperties );
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) {
                return true;
            }
            if ( !( other instanceof ParentsOrChildrenCacheKey ) ) {
                return false;
            }
            ParentsOrChildrenCacheKey that = ( ParentsOrChildrenCacheKey ) other;
            return direct == that.direct && includeAdditionalProperties == that.includeAdditionalProperties && ontologyService.equals( that.ontologyService ) && termsHash == that.termsHash && terms.equals( that.terms );
        }

        @Override
        public String toString() {
            return String.format( "%d terms from %s [%s] [%s]", terms.size(), ontologyService, direct ? "direct" : "all", includeAdditionalProperties ? "subClassOf and " + ontologyService.getAdditionalPropertyUris().size() + " additional properties" : "only subClassOf" );
        }
    }

    @Value
    private static class SearchCacheKey {
        OntologyService ontologyService;
        String query;
    }
}
