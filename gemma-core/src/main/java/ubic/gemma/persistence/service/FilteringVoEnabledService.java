package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface VO-enabled service with filtering capabilities.
 */
public interface FilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledService<O, VO>, FilteringService<O> {

    /**
     * @see FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)
     */
    Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * @see FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort)
     */
    List<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort );
}