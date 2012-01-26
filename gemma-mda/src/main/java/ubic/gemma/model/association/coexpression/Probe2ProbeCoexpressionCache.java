package ubic.gemma.model.association.coexpression;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.CoexpressionCacheValueObject;
import ubic.gemma.model.genome.Gene;

public interface Probe2ProbeCoexpressionCache {

    /**
     * @return the enabled
     */
    public abstract Boolean isEnabled();

    /**
     * @param enabled the enabled to set
     */
    public abstract void setEnabled( Boolean enabled );

    /**
     * @param coExVOForCache
     */
    public abstract void addToCache( CoexpressionCacheValueObject coExVOForCache );

    /**
     * 
     */
    public abstract void clearCache();

    /**
     * Remove all elements from the cache for the given expression experiment, if the cache exists.
     * 
     * @param e the expression experiment - specific cache to be cleared.
     */
    public abstract void clearCache( Long eeid );

    /**
     * @param ee
     * @param g
     * @return
     */
    public abstract Collection<CoexpressionCacheValueObject> get( BioAssaySet ee, Gene g );

}