package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;

public interface ProcessedDataVectorCache {

    public abstract void clearCache();

    /**
     * @param ee
     * @param g
     * @return
     */
    public abstract Collection<DoubleVectorValueObject> get( BioAssaySet ee, Gene g );

    /**
     * @param eeid
     * @param g
     * @param collection
     */
    public abstract void addToCache( Long eeid, Gene g, Collection<DoubleVectorValueObject> collection );

    /**
     * Remove cached items for experiment with given id.
     * 
     * @param eeid
     */
    public abstract void clearCache( Long eeid );

}