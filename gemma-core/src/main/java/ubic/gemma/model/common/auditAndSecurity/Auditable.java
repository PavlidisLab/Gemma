package ubic.gemma.model.common.auditAndSecurity;

import gemma.gsec.model.Securable;
import ubic.gemma.model.common.Identifiable;

/**
 * Created by tesarst on 07/03/17.
 * <p>
 * Interface that covers objects that are Auditable.
 * When creating new Auditable entity, new AuditTrail is automatically created in {@link ubic.gemma.persistence.persister.PersisterHelper}
 */
public interface Auditable extends Identifiable, Securable {

    ubic.gemma.model.common.auditAndSecurity.AuditTrail getAuditTrail();

    void setAuditTrail( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );
}
