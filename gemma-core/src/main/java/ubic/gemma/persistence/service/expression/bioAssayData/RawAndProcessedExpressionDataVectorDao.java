package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

public interface RawAndProcessedExpressionDataVectorDao extends DesignElementDataVectorDao<BulkExpressionDataVector> {

    /**
     * Remove all raw and processed vectors for a given probe.
     * @return the number of removed raw and processed vectors
     */
    int removeByCompositeSequence( CompositeSequence cs );
}
