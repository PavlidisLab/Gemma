package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Service to manipulate the "needs attention" flag on {@link FactorValue}s.
 *
 * <p>As of the {@code CURATION_DETAILS_RETIREMENT.md} Phase 1 follow-on
 * (2026-05-19) this service is implemented on top of the Ticket layer
 * ({@link ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService}).
 * {@link #markAsNeedsAttention} opens a
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#GENERIC}
 * ticket targeting both the FV and its owning EE;
 * {@link #clearNeedsAttentionFlag} transitions every open ticket targeting
 * the FV to
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketState#RESOLVED}.
 * The legacy
 * {@link ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent}
 * /
 * {@link ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent}
 * emissions are no longer fired from this path — see
 * {@code CURATION_DETAILS_RETIREMENT.md} for the read-side migration that
 * will eventually retire the embedded {@code CurationDetails} flags.</p>
 *
 * <p>New callers should use
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService#openTicket}
 * /
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService#transition}
 * directly; this service is retained as a thin convenience wrapper while
 * its existing callers (web controller, FactorValue migrator) are
 * migrated.</p>
 *
 * @author poirigui
 */
public interface FactorValueNeedsAttentionService {

    /**
     * Mark a given factor value as needs attention.
     *
     * <p>Opens a
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#GENERIC}
     * ticket whose targets are BOTH the factor value and the owning
     * expression experiment, with {@code note} appended to the ticket
     * title.</p>
     *
     * @param factorValue a factor value to mark as needs attention
     * @param note        human note used as the ticket title suffix
     * @throws IllegalArgumentException if the factor value already needs attention
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void markAsNeedsAttention( FactorValue factorValue, String note );

    /**
     * Clear a needs attention flag on a given factor value.
     *
     * <p>Transitions every open ticket targeting this factor value to
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketState#RESOLVED},
     * using {@code note} as the transition reason. Sibling factor values
     * on the same EE that still need attention keep their own open
     * tickets — the aggregate "EE has any open ticket?" query handles
     * the cross-entity rollup without a per-EE predicate.</p>
     *
     * @param factorValue a factor value whose needs flag will be cleared
     * @param note        transition reason for the ticket resolution
     * @throws IllegalArgumentException if the factor value does not need attention
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void clearNeedsAttentionFlag( FactorValue factorValue, String note );
}
