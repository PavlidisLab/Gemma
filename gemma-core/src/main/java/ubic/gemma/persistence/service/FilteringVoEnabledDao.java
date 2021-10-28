package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

/**
 * Interface for VO-enabled DAO with filtering capabilities.
 *
 * @param <O>
 * @param <VO>
 */
public interface FilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends FilteringDao<O>, BaseVoEnabledDao<O, VO> {

    /**
     * Load VOs with ordering, filtering and offset/limit.
     *
     * @param filters filters
     * @param sort an object property to order by
     * @param offset an offset to which
     * @param limit a limit on the number of returned results, or -1 to ignore
     * @return a slice of the relevant data
     */
    Slice<VO> loadValueObjectsPreFilter( Filters filters, Sort sort, int offset, int limit );

    /**
     * Load VOs with minimal ordering and filtering.
     *
     * Use this as an alternative to {@link #loadValueObjectsPreFilter(Filters, Sort, int, int)} if you do not
     * intend to provide pagination capabilities.
     *
     * @param filters  the filters that are applied
     * @param sort
     * @returns
     */
    List<VO> loadValueObjectsPreFilter( Filters filters, Sort sort );
}
