package ubic.gemma.model.association.coexpression;

import net.sf.ehcache.Cache;

public interface Gene2GeneCoexpressionCache {

    /**
     * Remove all elements from the cache.
     */
    public abstract void clearCache();

    public abstract Cache getCache();

}