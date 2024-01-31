package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssayData.RawOrProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.CheckReturnValue;
import java.util.Collection;

public interface RawAndProcessedExpressionDataVectorService extends DesignElementDataVectorService<RawOrProcessedExpressionDataVector> {

    /**
     * @see RawAndProcessedExpressionDataVectorDao#removeByCompositeSequence(CompositeSequence)
     */
    @Secured({ "GROUP_ADMIN" })
    int removeByCompositeSequence( CompositeSequence cs );

    /**
     * Thaw both raw and processed vectors.
     * <p>
     * This method thaws by first splitting the vectors into raw and processed groups and then thawing each group with
     * the corresponding vector service. If you know the type of vector a collection holds, favour using a specific
     * service instead.
     */
    @CheckReturnValue
    Collection<RawOrProcessedExpressionDataVector> thaw( Collection<RawOrProcessedExpressionDataVector> vectors );
}
