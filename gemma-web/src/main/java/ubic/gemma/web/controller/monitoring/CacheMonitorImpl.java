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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Get statistics about and manage caches.
 *
 * @author paul
 */
@Component
public class CacheMonitorImpl implements CacheMonitor, InitializingBean {

    private static final Log log = LogFactory.getLog( CacheMonitorImpl.class );

    /**
     * Header used when displaying cache statistics.
     */
    private static final String[] CACHE_STATS_HEADER = {
            "Name", "Hit Rate", "Hits", "Misses", "Evictions", "Average Get Time", "Count", "Capacity", "Usage",
            "Memory Usage", "Memory Usage per Element", "Use Disk?", "Life Time", "Idle Time" };

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ServletContext servletContext;

    @Value("${gemma.cacheMonitor.enableStatistics}")
    private boolean enableStatisticsOnStartup;

    /**
     * Cache for caching the result of {@link Ehcache#calculateInMemorySize()}.
     */
    private Cache inMemoryCacheSizeCache;

    @Override
    public void afterPropertiesSet() {
        inMemoryCacheSizeCache = cacheManager.getCache( "CacheMonitor.inMemoryCacheSizeCache" );
        if ( enableStatisticsOnStartup ) {
            enableStatistics();
        }
    }

    @Override
    public void clearAllCaches() {
        CacheMonitorImpl.log.info( "Clearing all caches" );
        for ( String cacheName : cacheManager.getCacheNames() ) {
            cacheManager.getCache( cacheName ).clear();
        }
    }

    @Override
    public void clearCache( String cacheName ) {
        Cache cache = this.cacheManager.getCache( cacheName );
        if ( cache != null ) {
            cache.clear();
            CacheMonitorImpl.log.info( "Cleared cache: " + cache.getName() );
        } else {
            throw new IllegalArgumentException( "No cache found with name=" + cacheName );
        }
    }

    @Override
    public void disableStatistics() {
        CacheMonitorImpl.log.info( "Disabling statistics for all caches..." );
        this.setStatisticsEnabled( false );
    }

    @Override
    public void enableStatistics() {
        CacheMonitorImpl.log.info( "Enabling statistics for all caches..." );
        this.setStatisticsEnabled( true );
    }

    @Override
    public String getStats( Locale locale ) {
        StringBuilder buf = new StringBuilder();
        List<String> cacheNames = cacheManager.getCacheNames().stream()
                .sorted()
                .collect( Collectors.toList() );

        buf.append( "<p>" )
                .append( cacheNames.size() ).append( " caches; only non-empty caches listed below." )
                .append( "</p>" );

        String anticlockwiseIconUrl = servletContext.getContextPath() + "/images/icons/arrow_rotate_anticlockwise.png";

        buf.append( "<p>" )
                .append( "To clear all caches click here: " )
                .append( "<img src=\"" ).append( anticlockwiseIconUrl ).append( "\" " )
                .append( "onClick=\"clearAllCaches()\" alt=\"Flush caches\" title=\"Clear caches\"/>" )
                .append( "</p>" );
        buf.append( "<p>" )
                .append( "To start statistics collection click here: " )
                .append( "<img src=\"" ).append( anticlockwiseIconUrl ).append( "\" " )
                .append( "onClick=\"enableStatistics()\" alt=\"Enable stats\" title=\"Enable stats\"/>" )
                .append( "</p>" );
        buf.append( "<p>" )
                .append( "To stop statistics collection click here: " )
                .append( "<img src=\"" ).append( anticlockwiseIconUrl ).append( "\" " )
                .append( "onClick=\"disableStatistics()\" alt=\"Disable stats\" title=\"Disable stats\"/>" )
                .append( "</p>" );

        buf.append( "<table style=\"width: 100%;\">" );
        int rowCount = 0;
        for ( String rawCacheName : cacheNames ) {
            if ( rowCount % 25 == 0 ) {
                addHeaderRow( buf );
                rowCount += 1;
            }
            if ( addCacheRow( rawCacheName, cacheManager.getCache( rawCacheName ), buf, locale ) ) {
                rowCount += 1;
            }
        }
        buf.append( "</table>" );
        return buf.toString();

    }

    private void addHeaderRow( StringBuilder buf ) {
        buf.append( "<tr>" );
        for ( String header : CACHE_STATS_HEADER ) {
            String align;
            if ( header.equals( "Name" ) ) {
                align = "left";
            } else if ( header.endsWith( "?" ) ) {
                align = "center";
            } else {
                align = "right";
            }
            buf.append( "<th style=\"text-align: " ).append( align ).append( ";\">" ).append( header ).append( "</th>" );
        }
        buf.append( "</tr>" );
    }

    private boolean addCacheRow( String rawCacheName, Cache cache, StringBuilder buf, Locale locale ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return addEhcacheRow( rawCacheName, ( Ehcache ) cache.getNativeCache(), buf, locale );
        } else {
            return false;
        }
    }

    private boolean addEhcacheRow( String rawCacheName, Ehcache cache, StringBuilder buf, Locale locale ) {
        Statistics statistics = cache.getStatistics();

        long objectCount = statistics.getObjectCount();

        if ( objectCount == 0 ) {
            return false;
        }

        // a little shorter...
        String cacheName = rawCacheName
                .replaceFirst( "^gemma\\.gsec\\.acl\\.domain\\.", "g.g.a.d." )
                .replaceFirst( "^ubic\\.gemma\\.model\\.", "u.g.m." );

        String anticlockwiseIconUrl = servletContext.getContextPath() + "/images/icons/arrow_rotate_anticlockwise.png";
        buf.append( "<tr>" )
                .append( "<td>" )
                .append( "<img src=\"" ).append( anticlockwiseIconUrl ).append( "\" onClick=\"clearCache('" ).append( escapeHtml4( rawCacheName ) ).append( "')\" alt=\"Clear cache\" title=\"Clear cache\"/> " )
                .append( escapeHtml4( cacheName ) )
                .append( "</td>" );

        long hits = statistics.getCacheHits();
        long misses = statistics.getCacheMisses();
        long maxSize = cache.getCacheConfiguration().getMaxElementsInMemory();
        long inMemorySize = getInMemorySize( cache );

        long evictions = statistics.getEvictionCount();

        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        boolean usesDisk = cacheConfiguration.isOverflowToDisk();
        boolean eternal = cacheConfiguration.isEternal();

        if ( usesDisk ) {
            maxSize += cache.getCacheConfiguration().getMaxElementsOnDisk();
        }

        NumberFormat percentFormat = NumberFormat.getPercentInstance( locale );
        NumberFormat numberFormat = NumberFormat.getNumberInstance( locale );

        buf.append( "<td style=\"text-align: right;\">" ).append( hits > 0 ? percentFormat.format( ( double ) hits / ( hits + misses ) ) : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( hits > 0 ? numberFormat.format( hits ) : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( misses > 0 ? numberFormat.format( misses ) : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( evictions > 0 ? numberFormat.format( evictions ) : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( hits > 0 ? numberFormat.format( statistics.getAverageGetTime() ) + " ms" : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( numberFormat.format( objectCount ) ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( numberFormat.format( maxSize ) ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( percentFormat.format( ( double ) objectCount / ( double ) maxSize ) ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( cache.getMemoryStoreSize() > 0 ? FileUtils.byteCountToDisplaySize( inMemorySize ) : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: right;\">" ).append( cache.getMemoryStoreSize() > 0 ? FileUtils.byteCountToDisplaySize( inMemorySize / cache.getMemoryStoreSize() ) : "" ).append( "</td>" );
        buf.append( "<td style=\"text-align: center;\">" ).append( usesDisk ? "&check;" : "&cross;" ).append( "</td>" );

        if ( eternal ) {
            // timeouts are irrelevant.
            buf.append( "<td style=\"text-align: center;\" colspan=\"2\">&infin;</td>" );
        } else {
            buf.append( "<td style=\"text-align: right;\">" ).append( cacheConfiguration.getTimeToLiveSeconds() > 0 ? numberFormat.format( cacheConfiguration.getTimeToLiveSeconds() ) + " s" : "" ).append( "</td>" );
            buf.append( "<td style=\"text-align: right;\">" ).append( cacheConfiguration.getTimeToIdleSeconds() > 0 ? numberFormat.format( cacheConfiguration.getTimeToIdleSeconds() ) + " s" : "" ).append( "</td>" );
        }

        buf.append( "</tr>" );

        return true;
    }

    @lombok.Value(staticConstructor = "from")
    private static class Entry {
        long memoryStoreSize;
        long inMemorySize;
    }

    private long getInMemorySize( Ehcache ehcache ) {
        String key = ehcache.getName();
        Cache.ValueWrapper value = inMemoryCacheSizeCache.get( key );
        long inMemorySize;
        if ( value != null ) {
            Entry entry = ( Entry ) value.get();
            // when the number of items in-memory changes, we adjust it proportionally
            // TTL is short and TTI is zero, so we don't need to worry about stale data
            inMemorySize = entry.inMemorySize * ehcache.getMemoryStoreSize() / entry.memoryStoreSize;
        } else {
            inMemorySize = ehcache.calculateInMemorySize();
            inMemoryCacheSizeCache.put( key, Entry.from( ehcache.getMemoryStoreSize(), inMemorySize ) );
        }
        return inMemorySize;
    }

    private void setStatisticsEnabled( boolean b ) {
        for ( String rawCacheName : cacheManager.getCacheNames() ) {
            Cache cache = cacheManager.getCache( rawCacheName );
            if ( cache.getNativeCache() instanceof Ehcache ) {
                ( ( Ehcache ) cache.getNativeCache() ).setStatisticsEnabled( b );
            }
        }
    }
}
