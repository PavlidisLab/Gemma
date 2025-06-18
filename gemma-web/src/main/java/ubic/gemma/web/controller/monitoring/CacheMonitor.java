/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.controller.monitoring;

import java.util.Locale;

/**
 * @author paul
 */
public interface CacheMonitor {

    /**
     * Remove all items from all caches.
     */
    void clearAllCaches();

    /**
     * Remove all items from the cache with the given name.
     *
     * @param cacheName cache
     * @throws IllegalArgumentException if no such cache exist with the given name
     */
    void clearCache( String cacheName ) throws IllegalArgumentException;

    /**
     * Disable collection of statistics on the caches.
     */
    void disableStatistics();

    /**
     * Enable collection of statistics on the caches.
     */
    void enableStatistics();

    /**
     * Obtain the cache statistics in HTML format.
     * @param locale a locale to use to format numbers and render messages
     */
    String getStats( Locale locale );
}