/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.persistence;

import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.Timestamper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Based on code by Les Hazlewood. http://www.leshazlewood.com/?p=37
 * 
 * @author paul
 * @version $Id$
 */
@Deprecated
@Component
public class ExternalCacheProvider implements CacheProvider {
    protected transient final Log log = LogFactory.getLog( getClass() );

    @Autowired
    private CacheManager cacheManager = null;

    public Cache buildCache( String name, Properties properties ) throws CacheException {
        try {
            Ehcache cache = cacheManager.getEhcache( name );
            if ( cache == null ) {
                if ( log.isWarnEnabled() ) {
                    log.warn( "Unable to find EHCache configuration for cache named [" + name + "]. Using defaults." );
                }
                cacheManager.addCache( name );
                cache = cacheManager.getEhcache( name );
                if ( log.isDebugEnabled() ) {
                    log.debug( "Started EHCache region '" + name + "'" );
                }
            }
            return new net.sf.ehcache.hibernate.EhCache( cache );
        } catch ( net.sf.ehcache.CacheException e ) {
            throw new CacheException( e );
        }
    }

    public boolean isMinimalPutsEnabledByDefault() {
        return false;
    }

    public long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * * This is the method that is called by an external framework (e.g. Spring) to set the * constructed CacheManager
     * for all instances of this class. Therefore, when * Hibernate instantiates this class, the previously statically
     * injected CacheManager * will be used for all hibernate calls to build caches. * @param cacheManager the
     * CacheManager instance to use for a HibernateSession factory using * this class as its cache.provider_class.
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    public void start( Properties properties ) throws CacheException {
        // ignored, CacheManager lifecycle handled by the IoC container
    }

    public void stop() {
        // ignored, CacheManager lifecycle handled by the IoC container
    }
}
