/*
 * The Gemma project Copyright (c) 2008 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.web.util.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.events.*;
import com.opensymphony.oscache.extra.ScopeEventListenerImpl;

/**
 * A simple implementation of a statistic reporter which uses the CacheMapAccessEventListener, CacheEntryEventListener
 * and ScopeEventListener. It uses the events to count the cache hit and misses and of course the flushes.
 * <p>
 * We are not using any synchronized so that this does not become a bottleneck. The consequence is that on retrieving
 * values, the operations that are currently being done won't be counted.
 */
public class PageCacheListener implements CacheMapAccessEventListener, CacheEntryEventListener, ScopeEventListener {

    private static transient final Log log = LogFactory.getLog( PageCacheListener.class );

    /**
     * Hit counter
     */
    private int hitCount = 0;

    /**
     * Miss counter
     */
    private int missCount = 0;

    /**
     * Stale hit counter
     */
    private int staleHitCount = 0;

    /**
     * Hit counter sum
     */
    private int hitCountSum = 0;

    /**
     * Miss counter sum
     */
    private int missCountSum = 0;

    /**
     * Stale hit counter
     */
    private int staleHitCountSum = 0;

    /**
     * Flush hit counter
     */
    private int flushCount = 0;

    /**
     * Constructor, empty for us
     */
    public PageCacheListener() {
        log.info( "Creation of PageCacheListener" );
    }

    /**
     * This method handles an event each time the cache is accessed
     * 
     * @param event The event triggered when the cache was accessed
     * @see com.opensymphony.oscache.base.events.CacheMapAccessEventListener#accessed(CacheMapAccessEvent)
     */
    public void accessed( CacheMapAccessEvent event ) {
        String result = "N/A";

        // Retrieve the event type and update the counters
        CacheMapAccessEventType type = event.getEventType();

        // Handles a hit event
        if ( type == CacheMapAccessEventType.HIT ) {
            hitCount++;

            result = "HIT";
            log.debug( "ACCESS : " + result + ": " + event.getCacheEntryKey() );
        }
        // Handles a stale hit event
        else if ( type == CacheMapAccessEventType.STALE_HIT ) {
            staleHitCount++;

            result = "STALE HIT";
            log.debug( "ACCESS : " + result + ": " + event.getCacheEntryKey() );
        }

        // Handles a miss event
        else if ( type == CacheMapAccessEventType.MISS ) {
            missCount++;
            result = "MISS";
        }

    }

    @Scheduled(fixedDelay = 10000)
    public void cacheStats() {
        log.debug( "Page Cache Stats : Hit = " + hitCount + ", stale hit =" + staleHitCount + ", miss = " + missCount );
    }

    /**
     * Logs the flush of the cache.
     * 
     * @param info the string to be logged.
     */
    private void flushed( String info ) {
        flushCount++;

        hitCountSum += hitCount;
        staleHitCountSum += staleHitCount;
        missCountSum += missCount;

        if ( log.isInfoEnabled() ) {
            log.info( "FLUSH : " + info );
            log.info( "STATISTIC SUM : " + "Hit = " + hitCountSum + ", stale hit = " + staleHitCountSum + ", miss = "
                    + missCountSum + ", flush = " + flushCount );
        }

        hitCount = 0;
        staleHitCount = 0;
        missCount = 0;
    }

    /**
     * Event fired when a specific or all scopes are flushed.
     * 
     * @param event ScopeEvent
     * @see com.opensymphony.oscache.base.events.ScopeEventListener#scopeFlushed(ScopeEvent)
     */
    public void scopeFlushed( ScopeEvent event ) {
        flushed( "scope " + ScopeEventListenerImpl.SCOPE_NAMES[event.getScope()] );
    }

    /**
     * Event fired when an entry is added to the cache.
     * 
     * @param event CacheEntryEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cacheEntryAdded(CacheEntryEvent)
     */
    public void cacheEntryAdded( CacheEntryEvent event ) {
        // do nothing
    }

    /**
     * Event fired when an entry is flushed from the cache.
     * 
     * @param event CacheEntryEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cacheEntryFlushed(CacheEntryEvent)
     */
    public void cacheEntryFlushed( CacheEntryEvent event ) {
        // do nothing, because a group or other flush is coming
        if ( !Cache.NESTED_EVENT.equals( event.getOrigin() ) ) {
            flushed( "entry " + event.getKey() + " / " + event.getOrigin() );
        }
    }

    /**
     * Event fired when an entry is removed from the cache.
     * 
     * @param event CacheEntryEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cacheEntryRemoved(CacheEntryEvent)
     */
    public void cacheEntryRemoved( CacheEntryEvent event ) {
        // do nothing
    }

    /**
     * Event fired when an entry is updated in the cache.
     * 
     * @param event CacheEntryEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cacheEntryUpdated(CacheEntryEvent)
     */
    public void cacheEntryUpdated( CacheEntryEvent event ) {
        // do nothing
    }

    /**
     * Event fired when a group is flushed from the cache.
     * 
     * @param event CacheGroupEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cacheGroupFlushed(CacheGroupEvent)
     */
    public void cacheGroupFlushed( CacheGroupEvent event ) {
        flushed( "group " + event.getGroup() );
    }

    /**
     * Event fired when a key pattern is flushed from the cache.
     * 
     * @param event CachePatternEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cachePatternFlushed(CachePatternEvent)
     */
    public void cachePatternFlushed( CachePatternEvent event ) {
        flushed( "pattern " + event.getPattern() );
    }

    /**
     * An event that is fired when an entire cache gets flushed.
     * 
     * @param event CachewideEvent
     * @see com.opensymphony.oscache.base.events.CacheEntryEventListener#cacheFlushed(CachewideEvent)
     */
    public void cacheFlushed( CachewideEvent event ) {
        flushed( "wide " + event.getDate() );
    }

    /**
     * Return the counters in a string form
     * 
     * @return String
     */
    @Override
    public String toString() {
        return "PageCacheListener: Hit = " + hitCount + " / " + hitCountSum + ", stale hit = " + staleHitCount + " / "
                + staleHitCountSum + ", miss = " + missCount + " / " + missCountSum + ", flush = " + flushCount;
    }
}
