package ubic.gemma.persistence.service;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

/**
 * Interface VO-enabled service with filtering capabilities.
 */
public interface FilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledService<O, VO>, FilteringService<O> {

    /**
     * @see FilteringVoEnabledDao#loadValueObjects(Filters, Sort, int, int)
     */
    Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * @see FilteringVoEnabledDao#loadValueObjects(Filters, Sort)
     */
    List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort );
}