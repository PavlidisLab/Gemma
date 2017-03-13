package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.beans.factory.InitializingBean;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.persistence.BaseDao;

/**
 * Created by tesarst on 13/03/17.
 */
public interface CurationDetailsDao extends InitializingBean, BaseDao<CurationDetails> {
    @Override
    CurationDetails load( Long id );

    CurationDetails create( AuditEvent createdEvent );

    void update( Curatable curatable, AuditEvent auditEvent );
}
