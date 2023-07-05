package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;

import java.util.Collection;
import java.util.List;

public interface CuratableService<C extends Curatable> {

    /**
     * Retain non-troubled IDs.
     */
    List<Long> retainNonTroubledIds( Collection<Long> ids );
}
