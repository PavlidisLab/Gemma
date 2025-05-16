package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Cache for processed data vectors.
 * @see ProcessedDataVectorByGeneCache
 * @see CachedProcessedExpressionDataVectorService
 */
interface ProcessedDataVectorCache {

    @Nullable
    Collection<DoubleVectorValueObject> get( ExpressionExperiment ee );

    void put( ExpressionExperiment ee, Collection<DoubleVectorValueObject> vectors );

    /**
     * Evict all the vectors attached to the given experiment.
     */
    void evict( ExpressionExperiment ee );

    void clear();
}
