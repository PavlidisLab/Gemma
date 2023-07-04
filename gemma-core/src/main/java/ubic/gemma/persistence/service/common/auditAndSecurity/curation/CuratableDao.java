package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.core.security.audit.IgnoreAudit;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;

import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 13/03/17.
 * DAO wrapper for all curatable DAOs.
 */
public interface CuratableDao<C extends Curatable> {

    Collection<C> findByName( String name );

    C findByShortName( String name );

    List<Long> retainNonTroubledIds( Collection<Long> ids );

    /**
     * This is marked as ignored for audit purposes since we don't want to audit the curation details update when it
     * originated from an audit event.
     */
    @IgnoreAudit
    void updateCurationDetailsFromAuditEvent( C curatable, AuditEvent auditEvent );
}
