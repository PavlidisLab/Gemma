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
package ubic.gemma.model.association.coexpression;

import ubic.gemma.util.ConfigUtils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * Configures the cache for gene2gene coexpression.
 * 
 * @author paul
 * @version $Id$
 */
public class Gene2GeneCoexpressionCache {

    private static final String GENE_COEXPRESSION_CACHE_NAME = "Gene2GeneCoexpressionCache";
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    private Gene2GeneCoexpressionCache() {
    }

    /**
     * Remove all elements from the cache.
     */
    public static void clearCache() {
        CacheManager manager = CacheManager.getInstance();
        manager.getCache( GENE_COEXPRESSION_CACHE_NAME ).removeAll();
    }

    /**
     * Initialize the vector cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    public static Cache initializeCache() {

        int maxElements = ConfigUtils.getInt( "gemma.cache.gene2gene.maxelements",
                GENE_COEXPRESSION_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.gene2gene.timetolive",
                GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.gene2gene.timetoidle",
                GENE_COEXPRESSION_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.gene2gene.usedisk",
                GENE_COEXPRESSION_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.gene2gene.eternal",
                GENE_COEXPRESSION_CACHE_DEFAULT_ETERNAL );

        /*
         * Create a cache for the probe data.s
         */
        CacheManager manager = CacheManager.getInstance();

        if ( manager.cacheExists( GENE_COEXPRESSION_CACHE_NAME ) ) {
            return manager.getCache( GENE_COEXPRESSION_CACHE_NAME );
        }

        Cache cache = new Cache( GENE_COEXPRESSION_CACHE_NAME, maxElements, overFlowToDisk, eternal, timeToLive,
                timeToIdle );

        manager.addCache( cache );
        return manager.getCache( GENE_COEXPRESSION_CACHE_NAME );
    }

}
