package ubic.gemma.core.search;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.persistence.util.CacheUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * High-level routines to interact with the ontology children cache.
 * @author poirigui
 */
@Component
@CommonsLog
public class OntologyChildrenCache implements InitializingBean {

    private static final String ONTOLOGY_CHILDREN_CACHE_NAME = "OntologyChildrenCache";

    @Autowired
    private CacheManager cacheManager;

    private Cache childTermCache;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.childTermCache = CacheUtils.getCache( cacheManager, ONTOLOGY_CHILDREN_CACHE_NAME );
    }

    /**
     * Get the children of a given term.
     */
    public Set<OntologyTerm> getChildren( OntologyTerm term ) {
        return getChildren( getDirectChildren( term ) );
    }

    /**
     * Get the children of a set of matching terms.
     * <p>
     * Note: some matching terms can be returned if they are children of others.
     */
    public Set<OntologyTerm> getChildren( Collection<OntologyTerm> matchingTerms ) {
        StopWatch timer = StopWatch.createStarted();
        Set<OntologyTerm> terms = new HashSet<>();
        // perform BFS to gather all the children, skipping visited terms
        Queue<OntologyTerm> fringe = new ArrayDeque<>( matchingTerms );
        Set<OntologyTerm> nonChildrenMatchingTerms = new HashSet<>( matchingTerms );
        while ( !fringe.isEmpty() ) {
            OntologyTerm term = fringe.remove();
            if ( terms.contains( term ) )
                continue; // already visited
            // this is cached and handle some quirks
            Collection<OntologyTerm> dc = getDirectChildren( term );
            fringe.addAll( dc );
            nonChildrenMatchingTerms.removeAll( dc );
            terms.add( term );
        }
        // only remove matching terms that are not children
        terms.removeAll( nonChildrenMatchingTerms );
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > 100 ) {
            log.warn( String.format( "Gathering children for %d terms took %d ms.",
                    matchingTerms.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return terms;
    }

    /**
     * Obtain the direct children of a term.
     */
    public Collection<OntologyTerm> getDirectChildren( OntologyTerm term ) {
        String uri = term.getUri();

        Collection<OntologyTerm> children;
        if ( StringUtils.isBlank( uri ) ) {
            // shouldn't happen, but just in case
            log.warn( "Blank URI for " + term );
            return Collections.emptySet();
        }

        Cache.ValueWrapper cachedChildren = this.childTermCache.get( uri );
        if ( cachedChildren == null ) {
            try {
                children = term.getChildren( true );
                childTermCache.put( uri, children );
            } catch ( com.hp.hpl.jena.ontology.ConversionException ce ) {
                log.warn( String.format( "Getting children for term: %s caused a Jena conversion exception. No children will be returned as a result.", term ), ce );
                return Collections.emptySet();
            }
        } else {
            //noinspection unchecked
            children = ( Collection<OntologyTerm> ) cachedChildren.get();
        }

        return children;
    }
}
