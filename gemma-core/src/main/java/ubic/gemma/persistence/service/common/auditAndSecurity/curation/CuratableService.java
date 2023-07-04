package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

public interface CuratableService<C extends Curatable> {

    /**
     * Update the curation details of a given curatable entity.
     * <p>
     * This method should only be called from {@link AuditTrailService}, as the passed event has to already exist in the
     * audit trail of the curatable object.
     * <p>
     * Only use this method directly if you do not want the event to show up in the curatable objects audit trail.
     *
     * @param curatable  curatable
     * @param auditEvent the event containing information about the update. Method only accepts audit events whose type
     *                   is one of {@link CurationDetailsEvent} extensions.
     * @see CuratableDao#updateCurationDetailsFromAuditEvent(Curatable, AuditEvent)
     */
    @Secured({ "GROUP_AGENT", "ACL_SECURABLE_EDIT" })
    void updateCurationDetailsFromAuditEvent( C curatable, AuditEvent auditEvent );
}
