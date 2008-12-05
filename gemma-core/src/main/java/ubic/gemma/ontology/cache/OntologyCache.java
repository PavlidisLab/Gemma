/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.ontology.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import ubic.gemma.ontology.OntologyResource;

/**
 * @author pavlidis
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class OntologyCache {

    private static final int TIME_TO_LIVE = 10000;
    private static final int MAX_ELEMENTS = 50000;
    private Cache cache;

    public OntologyCache() {
        try {

            // fixme: get this from the spring context?
            CacheManager manager = CacheManager.getInstance();

            cache = new Cache( "ontologyCache", MAX_ELEMENTS, false, true, TIME_TO_LIVE, 30, false, 0 );

            manager.addCache( cache );
            cache = manager.getCache( "ontologyCache" );

        } catch ( CacheException e ) {
            throw new RuntimeException();
        }
    }

    /**
     * @param uri
     * @return
     * @throws IllegalStateException
     * @throws CacheException
     */
    public OntologyResource get( String uri ) throws IllegalStateException, CacheException {
        Element element = cache.get( uri );
        if ( element == null ) {
            throw new IllegalArgumentException( "Nothing in cache for " + uri );
        }
        return ( OntologyResource ) element.getValue();
    }

    /**
     * @param term
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    public void put( OntologyResource term ) throws IllegalArgumentException, IllegalStateException {
        if ( term == null ) return;
        assert cache != null;
        cache.put( new Element( term.getUri(), term ) );
    }

}
