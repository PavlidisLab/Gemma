package ubic.gemma.persistence.service;

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface for VO-enabled DAO with filtering capabilities.
 * @author poirigui
 */
public interface FilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledDao<O, VO>, FilteringDao<O> {

    /**
     * Load VOs with ordering, filtering and offset/limit.
     * <p>
     * Consider using {@link #getFilter(String, Filter.Operator, String)} and {@link #getSort(String, Sort.Direction)}
     * to produce the filters and sort safely from user input.
     *
     * @see #load(Filters, Sort, int, int)
     *
     * @param filters filters applied on the search. The properties mentioned in the {@link Filter}
     *                must exist and be visible to Hibernate. You can use nested properties such as "curationDetails.lastUpdated".
     * @param sort    an object property and direction to order by. This property must exist and be visible to
     *                Hibernate. You can use nested properties such as "curationDetails.lastUpdated".
     * @param offset  an offset from which entities are retrieved when sorted according to the sort argument, or 0 to
     *                ignore
     * @param limit   a limit on the number of returned results, or -1 to ignore
     * @return a slice of the relevant data
     */
    Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * Load VOs with minimal ordering and filtering.
     * <p>
     * Use this as an alternative to {@link #loadValueObjects(Filters, Sort, int, int)} if you do not
     * intend to provide pagination capabilities.
     *
     * @see #load(Filters, Sort)
     * @see #loadValueObjects(Filters, Sort, int, int)
     */
    List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort );
}
