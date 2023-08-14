package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author poirigui
 */
public interface CachedFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends FilteringVoEnabledDao<O, VO>, CachedFilteringDao<O> {

    List<VO> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort );

    Slice<VO> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );
}
