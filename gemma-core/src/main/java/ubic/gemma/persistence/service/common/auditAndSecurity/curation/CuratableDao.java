package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.List;

/**
 * Created by tesarst on 13/03/17.
 * DAO wrapper for all curatable DAOs.
 */
public interface CuratableDao<C extends Curatable> {

    /**
     * Load IDs of troubled entities.
     */
    List<Long> loadTroubledIds();

    /**
     * Update the curation details of a given curatable entity.
     * <p>
     * This method should only be called from {@link AuditTrailService}, as the passed event has to already exist in the
     * audit trail of the curatable object.
     * <p>
     * Only use this method directly if you do not want the event to show up in the curatable objects audit trail.
     * <p>
     * Historically this method was marked {@code @IgnoreAudit} to opt out of the generic
     * auto-UPDATE aspect that fired on every {@code update*} DAO method. That aspect was
     * retired in Audit Phase C (terminal step); the annotation is no longer needed because
     * there is no longer a blanket DAO-method audit aspect to opt out of.
     *
     * @param curatable  curatable
     * @param auditEvent the event containing information about the update. Method only accepts audit events whose type
     *                   is one of {@link CurationDetailsEvent} extensions.
     * @see CuratableDao#updateCurationDetailsFromAuditEvent(Curatable, AuditEvent)
     */
    void updateCurationDetailsFromAuditEvent( C curatable, AuditEvent auditEvent );
}
