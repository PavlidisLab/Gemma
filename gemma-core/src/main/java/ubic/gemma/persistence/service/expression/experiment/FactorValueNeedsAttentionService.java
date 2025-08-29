package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Service to manipulate the "needs attention" flag on {@link FactorValue}s.
 * @author poirigui
 */
public interface FactorValueNeedsAttentionService {

    /**
     * Mark a given factor value as needs attention.
     * @param factorValue a factor value to mark as needs attention
     * @param note note to use for the {@link ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent}
     * @throws IllegalArgumentException if the factor value already needs attention
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void markAsNeedsAttention( FactorValue factorValue, String note );

    /**
     * Clear a needs attention flag on a given factor value.
     * @param factorValue a factor value whose needs flag will be cleared
     * @param note a note to use for the {@link ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent}
     *             if the dataset does not need attention for any other reason.
     * @throws IllegalArgumentException if the factor value does not need attention
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void clearNeedsAttentionFlag( FactorValue factorValue, String note );
}
