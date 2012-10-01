package ubic.gemma.model.genome.gene;

import net.sf.ehcache.Cache;

public interface GeneLightWeightCache {

    /**
     * Remove all elements from the cache.
     */
    public abstract void clearCache();

    public abstract Cache getCache();

}