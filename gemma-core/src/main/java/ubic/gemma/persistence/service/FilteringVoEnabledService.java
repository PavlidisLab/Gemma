package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import java.util.List;

/**
 * Interface VO-enabled service with filtering capabilities.
 *
 * @param <O>
 * @param <VO>
 */
public interface FilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends FilteringService<O>, BaseVoEnabledService<O, VO> {

    /**
     * @see FilteringVoEnabledDao#loadValueObjectsPreFilter(List, String, boolean, int, int)
     */
    Slice<VO> loadValueObjectsPreFilter( List<ObjectFilter[]> filter, String orderBy, boolean asc, int offset, int limit );

    /**
     * @see FilteringVoEnabledDao#loadValueObjectsPreFilter(List, String, boolean)
     */
    List<VO> loadValueObjectsPreFilter( List<ObjectFilter[]> filter, String orderBy, boolean asc );
}