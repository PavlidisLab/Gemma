package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.persistence.service.BaseDao;

/**
 * Created by tesarst on 13/03/17.
 *
 * Interface extracted from CurationDetailsDaoImpl to satisfy spring autowiring requirements.
 */
@Transactional
public interface CurationDetailsDao extends BaseDao<CurationDetails> {
    @Override
    CurationDetails load( Long id );

    CurationDetails create();

    void update( Curatable curatable, AuditEvent auditEvent );
}
