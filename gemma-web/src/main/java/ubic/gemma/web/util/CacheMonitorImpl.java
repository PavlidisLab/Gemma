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
package ubic.gemma.web.util;

import lombok.SneakyThrows;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Get statistics about and manage caches.
 *
 * @author paul
 */
@Component
public class CacheMonitorImpl implements CacheMonitor {

    private static final Log log = LogFactory.getLog( CacheMonitorImpl.class );

    /**
     * Header used when displaying cache statistics.
     */
    private static final String CACHE_STATS_HEADER = "<th>Name</th><th>HitRate</th><th>Hits</th><th>Misses</th><th>Count</th><th>MemHits</th><th>MemMiss</th><th>DiskHits</th><th>Evicted</th><th>Eternal?</th><th>UseDisk?</th><th>MaxInMem</th><th>LifeTime</th><th>IdleTime</th>";

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ServletContext servletContext;

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
        CacheMonitorImpl.log.info( "Disabling statistics" );
        this.setStatisticsEnabled( false );
    }

    @Override
    public void enableStatistics() {
        CacheMonitorImpl.log.info( "Enabling statistics" );
        this.setStatisticsEnabled( true );
    }

    @Override
    public String getStats() {
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

        buf.append( "<table>" );
        buf.append( "<tr>" )
                .append( CACHE_STATS_HEADER )
                .append( "</tr>" );
        int count = 0;
        for ( String rawCacheName : cacheNames ) {
            Cache cache = cacheManager.getCache( rawCacheName );
            if ( cache.getNativeCache() instanceof Ehcache ) {
                addEhcacheRow( rawCacheName, ( Ehcache ) cache.getNativeCache(), buf );
            }
            if ( ++count % 25 == 0 ) {
                buf.append( "<tr>" ).append( CACHE_STATS_HEADER ).append( "</tr>" );
            }
        }
        buf.append( "</table>" );
        return buf.toString();

    }

    @SneakyThrows(IOException.class)
    private void addEhcacheRow( String rawCacheName, Ehcache cache, Appendable buf ) {
        Statistics statistics = cache.getStatistics();

        long objectCount = statistics.getObjectCount();

        if ( objectCount == 0 ) {
            return;
        }

        // a little shorter...
        String cacheName = rawCacheName.replaceFirst( "ubic\\.gemma\\.model\\.", "u.g.m." );

        String anticlockwiseIconUrl = servletContext.getContextPath() + "/images/icons/arrow_rotate_anticlockwise.png";
        buf.append( "<tr><td>" )
                .append( "<img src=\"" ).append( anticlockwiseIconUrl ).append( "\" onClick=\"clearCache('" ).append( escapeHtml4( rawCacheName ) ).append( "')\" alt=\"Clear cache\" title=\"Clear cache\"/> " )
                .append( escapeHtml4( cacheName ) )
                .append( "</td>" );
        long hits = statistics.getCacheHits();
        long misses = statistics.getCacheMisses();
        long inMemoryHits = statistics.getInMemoryHits();
        long inMemoryMisses = statistics.getInMemoryMisses();

        long onDiskHits = statistics.getOnDiskHits();
        long evictions = statistics.getEvictionCount();

        buf.append( this.makeTableCellForStat( ( double ) hits / ( hits + misses ) ) );
        buf.append( this.makeTableCellForStat( hits ) );
        buf.append( this.makeTableCellForStat( misses ) );
        buf.append( this.makeTableCellForStat( objectCount ) );
        buf.append( this.makeTableCellForStat( inMemoryHits ) );
        buf.append( this.makeTableCellForStat( inMemoryMisses ) );
        buf.append( this.makeTableCellForStat( onDiskHits ) );
        buf.append( this.makeTableCellForStat( evictions ) );

        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        boolean eternal = cacheConfiguration.isEternal();
        buf.append( "<td>" ).append( eternal ? "&bull;" : "" ).append( "</td>" );

        buf.append( "<td>" ).append( String.format( "%d", cacheConfiguration.getMaxElementsInMemory() ) ).append( "</td>" );

        if ( eternal ) {
            // timeouts are irrelevant.
            buf.append( "<td>-</td>" );
            buf.append( "<td>-</td>" );
        } else {
            buf.append( "<td>" ).append( String.format( "%d", cacheConfiguration.getTimeToIdleSeconds() ) ).append( "</td>" );
            buf.append( "<td>" ).append( String.format( "%d", cacheConfiguration.getTimeToLiveSeconds() ) ).append( "</td>" );
        }
        buf.append( "</tr>" );
    }

    private String makeTableCellForStat( long hits ) {
        return "<td>" + ( hits > 0 ? String.format( "%d", hits ) : "" ) + "</td>";
    }

    private String makeTableCellForStat( double hits ) {
        return "<td>" + ( hits > 0 ? String.format( "%.2f", hits ) : "" ) + "</td>";
    }

    private void setStatisticsEnabled( boolean b ) {
        for ( String rawCacheName : cacheManager.getCacheNames() ) {
            Cache cache = cacheManager.getCache( rawCacheName );
            if ( cache.getNativeCache() instanceof Ehcache ) {
                ( ( Ehcache ) cache.getNativeCache() ).setSampledStatisticsEnabled( b );
            }
        }
    }
}
