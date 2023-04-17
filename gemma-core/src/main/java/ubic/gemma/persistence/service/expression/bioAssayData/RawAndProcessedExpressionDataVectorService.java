package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

public interface RawAndProcessedExpressionDataVectorService extends DesignElementDataVectorService<DesignElementDataVector> {

    /**
     * @see RawAndProcessedExpressionDataVectorDao#removeByCompositeSequence(CompositeSequence)
     */
    @Secured({ "GROUP_ADMIN" })
    int removeByCompositeSequence( CompositeSequence cs );
}
