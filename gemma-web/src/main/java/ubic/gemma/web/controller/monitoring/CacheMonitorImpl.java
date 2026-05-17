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
package ubic.gemma.web.controller.monitoring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Stubbed cache monitor for the post-EhCache-2 era. Cache statistics UI is
 * disabled; clear-all and clear-named still work via the Spring CacheManager.
 */
@Component
public class CacheMonitorImpl implements CacheMonitor {

    private static final Log log = LogFactory.getLog( CacheMonitorImpl.class );

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void clearAllCaches() {
        log.info( "Clearing all caches" );
        for ( String cacheName : cacheManager.getCacheNames() ) {
            Cache cache = cacheManager.getCache( cacheName );
            if ( cache != null ) {
                cache.clear();
            }
        }
    }

    @Override
    public void clearCache( String cacheName ) {
        Cache cache = this.cacheManager.getCache( cacheName );
        if ( cache != null ) {
            cache.clear();
            log.info( "Cleared cache: " + cache.getName() );
        } else {
            throw new IllegalArgumentException( "No cache found with name=" + cacheName );
        }
    }

    @Override
    public void disableStatistics() {
        log.info( "Statistics are not available in this build." );
    }

    @Override
    public void enableStatistics() {
        log.info( "Statistics are not available in this build." );
    }

    @Override
    public String getStats( Locale locale ) {
        List<String> cacheNames = cacheManager.getCacheNames().stream()
                .sorted()
                .collect( Collectors.toList() );
        StringBuilder buf = new StringBuilder();
        buf.append( "<p>" )
                .append( cacheNames.size() )
                .append( " caches registered. Per-cache statistics are not available in this build.</p>" );
        buf.append( "<ul>" );
        for ( String name : cacheNames ) {
            buf.append( "<li>" ).append( name ).append( "</li>" );
        }
        buf.append( "</ul>" );
        return buf.toString();
    }
}
