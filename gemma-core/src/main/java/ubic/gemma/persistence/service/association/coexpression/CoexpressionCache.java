/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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
package ubic.gemma.persistence.service.association.coexpression;

import ubic.gemma.core.config.Settings;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Cache for coexpression results. Useful caching requires that the query be done at a suitably low stringency, defined
 * by CACHE_QUERY_STRINGENCY, against all data sets.
 *
 * @author Paul
 * @see CoexpressionQueryQueue
 */
public interface CoexpressionCache {

    /**
     * The stringency used to query when populating the cache. This can't be too low or the cache gets gigantic; if
     * someone is querying for data from a single dataset, they would use experiment-first mode.
     */
    int CACHE_QUERY_STRINGENCY = Settings.getInt( "gemma.cache.gene2gene.stringencyThreshold", 5 );

    void cacheCoexpression( Long geneId, Collection<CoexpressionValueObject> r );

    void cacheCoexpression( Map<Long, List<CoexpressionValueObject>> r );

    /**
     * Remove all elements from the cache.
     */
    @SuppressWarnings("unused")
    // Possible external use
    void clearCache();

    /**
     * @param g gene id
     * @return results sorted in descending order of support, or null if the gene was not in the cache
     */
    List<CoexpressionValueObject> get( Long g );

    boolean isEnabled();

    /**
     * Bulk remove from cache.
     *
     * @param genes genes
     * @return number of cache entries affected
     */
    int remove( Collection<Long> genes );

    @SuppressWarnings("UnusedReturnValue")
        // Possible external use
    boolean remove( Long id );

    void shutdown();

}