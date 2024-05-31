package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.Auditable;

/**
 * Created by tesarst on 06/03/17.
 * Interface that covers objects that are Curatable.
 * When creating new Curatable entity, Curation Details are automatically created in
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao}
 */
public interface Curatable extends Auditable {

    CurationDetails getCurationDetails();

    void setCurationDetails( CurationDetails curationDetails );

}
