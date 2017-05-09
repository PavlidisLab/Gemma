package ubic.gemma.model.common;

/**
 * Created by tesarst on 07/03/17.
 *
 * Interface that covers objects that are Auditable.
 * When creating new Auditable entity, new AuditTrail is automatically created in {@link ubic.gemma.persistence.persister.PersisterHelper}
 */
public interface Auditable {

    ubic.gemma.model.common.auditAndSecurity.AuditTrail getAuditTrail();

    void setAuditTrail( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );
}
