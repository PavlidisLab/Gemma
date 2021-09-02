package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import java.util.List;

/**
 * Interface for VO-enabled DAO with filtering capabilities.
 *
 * @param <O>
 * @param <VO>
 */
public interface FilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledDao<O, VO> {

    Slice<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter );
}
