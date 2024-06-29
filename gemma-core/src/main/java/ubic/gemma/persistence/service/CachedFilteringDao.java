package ubic.gemma.persistence.service;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

/**
 * @author poirigui
 */
public interface CachedFilteringDao<O extends Identifiable> extends FilteringDao<O> {

    /**
     * @see #loadIds(Filters, Sort)
     */
    List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * @see #load(Filters, Sort)
     */
    List<O> loadWithCache( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * @see #load(Filters, Sort, int, int)
     */
    Slice<O> loadWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * @see #count(Filters)
     */
    long countWithCache( @Nullable Filters filters );
}
