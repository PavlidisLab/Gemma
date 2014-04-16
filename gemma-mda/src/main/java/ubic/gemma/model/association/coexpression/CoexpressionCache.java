/*
 * The gemma-mda project
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
package ubic.gemma.model.association.coexpression;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Cache for coexpression results. Results are only cached for a gene if they are done at the minimum usable stringency
 * (2) for all data sets.
 * 
 * @author Paul
 * @version $Id$
 * @see CoexpressionQueryQueue
 */
public interface CoexpressionCache {

    /**
     * The stringency used to query when populatin the cache. This can't be too low or the cache gets gigantic; if
     * someone is querying for data from a single dataset, they would use experiment-first mode.
     */
    public static final int CACHE_QUERY_STRINGENCY = 3;

    /**
     * @param geneId
     * @param r
     */
    public void cacheCoexpression( Long geneId, Collection<CoexpressionValueObject> r );

    /**
     * @param r
     */
    public void cacheCoexpression( Map<Long, List<CoexpressionValueObject>> r );

    /**
     * Remove all elements from the cache.
     */
    public abstract void clearCache();

    /**
     * @param g
     * @return results sorted in descending order of support, or null if the gene was not in the cache
     */
    public abstract List<CoexpressionValueObject> get( Long g );

    /**
     * @param id
     * @return
     */
    public abstract boolean remove( Long id );

    /**
     * Bulk remove from cache.
     * 
     * @param genes
     * @return number of cache entries affected
     */
    public int remove( Collection<Long> genes );

}