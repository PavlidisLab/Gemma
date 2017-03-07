package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.model.common.Auditable;

/**
 * Created by tesarst on 06/03/17.
 *
 * Interface that covers objects that are Curatable
 */
public interface Curatable extends Auditable {

    CurationDetails getCurationDetails();

    void setCurationDetails( CurationDetails curationDetails );

}
