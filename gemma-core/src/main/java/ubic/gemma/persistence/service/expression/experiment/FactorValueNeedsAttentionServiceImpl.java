package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ticket-backed implementation of {@link FactorValueNeedsAttentionService}.
 *
 * <p>Per Decision 1 of {@code AUDIT_AS_WORKFLOW_RECCE.md} and the
 * follow-on migration tracked in {@code CURATION_DETAILS_RETIREMENT.md},
 * needs-attention bookkeeping is no longer recorded as
 * {@link ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent}
 * /
 * {@link ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent}
 * pairs on the EE's audit trail. Instead, each {@code markAsNeedsAttention}
 * opens a {@link TicketType#GENERIC} ticket whose targets are BOTH the
 * {@link TicketTargetType#FACTOR_VALUE} and its owning
 * {@link TicketTargetType#EXPRESSION_EXPERIMENT} (so the EE-level
 * "any open ticket?" lookup naturally picks it up), and
 * {@code clearNeedsAttentionFlag} transitions all open tickets targeting
 * this FV to {@link TicketState#RESOLVED}.</p>
 *
 * <p>The cross-entity "is this the last outstanding FV?" predicate from
 * the legacy implementation is no longer needed: the aggregate "EE needs
 * attention" question becomes
 * {@code ticketService.findOpenForTarget(EXPRESSION_EXPERIMENT, eeId)}
 * &ne; empty, which is automatically true as long as any sibling FV's
 * ticket remains open.</p>
 *
 * <p>{@link FactorValue#getNeedsAttention()} continues to be flipped
 * directly — it is a first-class field on the FV, not a CurationDetails-
 * managed projection, and other code (notably this class's own predicate)
 * reads it.</p>
 *
 * @author poirigui
 * @author paul (Ticket migration, 2026-05-19)
 */
@Service
public class FactorValueNeedsAttentionServiceImpl implements FactorValueNeedsAttentionService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserManager userManager;

    @Override
    @Transactional
    public void markAsNeedsAttention( FactorValue factorValue, String note ) {
        Assert.isTrue( !factorValue.getNeedsAttention(), "This FactorValue already needs attention." );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factorValue );
        factorValue.setNeedsAttention( true );
        factorValueService.update( factorValue );
        if ( ee != null ) {
            openNeedsAttentionTicket( factorValue, ee, note );
        }
    }

    @Override
    @Transactional
    public void clearNeedsAttentionFlag( FactorValue factorValue, String note ) {
        Assert.isTrue( factorValue.getNeedsAttention(), "This FactorValue does not need attention." );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factorValue );
        factorValue.setNeedsAttention( false );
        factorValueService.update( factorValue );
        if ( ee != null ) {
            resolveOpenNeedsAttentionTickets( factorValue, note );
        }
    }

    /**
     * Open a {@link TicketType#GENERIC} ticket targeting both the
     * {@code factorValue} and its owning {@code ee}. No-op if an open
     * GENERIC ticket already targets this factor value (idempotent
     * re-mark prevented).
     */
    private void openNeedsAttentionTicket( FactorValue factorValue, ExpressionExperiment ee, String note ) {
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            // The @Secured annotation on the interface already gates anonymous callers; this is a
            // defensive guard for the edge case where SecurityContext returns a non-User principal.
            throw new IllegalStateException( "No authenticated user resolved." );
        }
        for ( Ticket existing : ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, factorValue.getId() ) ) {
            if ( existing.getType() == TicketType.GENERIC ) {
                return;
            }
        }
        String title = String.format( "%s: %s", factorValue, note );
        Set<TicketTarget> targets = new HashSet<>( Arrays.asList(
                TicketTarget.Factory.newInstance( TicketTargetType.FACTOR_VALUE, factorValue.getId() ),
                TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ) );
        ticketService.openTicket( actor, TicketType.GENERIC, title, targets );
    }

    /**
     * Transition every open ticket targeting this {@code factorValue} to
     * {@link TicketState#RESOLVED}. Tickets that also target the owning
     * EE are resolved as a single unit — the EE-level "any open ticket?"
     * lookup naturally reflects remaining sibling-FV tickets without
     * needing the legacy "is this the last outstanding FV?" predicate.
     */
    private void resolveOpenNeedsAttentionTickets( FactorValue factorValue, String note ) {
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            throw new IllegalStateException( "No authenticated user resolved." );
        }
        List<Ticket> open = ticketService.findOpenForTarget( TicketTargetType.FACTOR_VALUE, factorValue.getId() );
        for ( Ticket t : open ) {
            if ( t.getState() != TicketState.RESOLVED && t.getState() != TicketState.CANCELLED ) {
                ticketService.transition( t, TicketState.RESOLVED, actor, note );
            }
        }
    }
}
