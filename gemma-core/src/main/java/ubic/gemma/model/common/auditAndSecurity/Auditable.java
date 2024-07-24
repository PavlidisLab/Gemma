package ubic.gemma.model.common.auditAndSecurity;


/**
 * Created by tesarst on 07/03/17.
 * <p>
 * Interface that covers objects that are Auditable.
 * When creating new Auditable entity, new AuditTrail is automatically created in {@link ubic.gemma.persistence.persister.PersisterHelper}
 */
public interface Auditable extends Securable {

    AuditTrail getAuditTrail();

    void setAuditTrail( AuditTrail auditTrail );
}
