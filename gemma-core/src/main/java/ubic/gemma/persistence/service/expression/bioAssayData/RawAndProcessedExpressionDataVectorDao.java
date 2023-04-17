package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.Collection;

public interface RawAndProcessedExpressionDataVectorDao extends DesignElementDataVectorDao<DesignElementDataVector> {

    /**
     * Remove all raw and processed vectors for a given probe.
     * @return the number of removed raw and processed vectors
     */
    int removeByCompositeSequence( CompositeSequence cs );
}
