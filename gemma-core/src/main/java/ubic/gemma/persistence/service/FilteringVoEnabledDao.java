package ubic.gemma.persistence.service;

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
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
     * Consider using {@link #getFilter(String, Filter.Operator, String)} and {@link FilteringDao#getSort(String, Sort.Direction, Sort.NullMode)}
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

    /**
     * Load VOs by keyset (cursor) pagination.
     * <p>
     * Cursor pagination avoids the O(N) scan-and-discard cost of
     * {@link #loadValueObjects(Filters, Sort, int, int)} on deep pages and is drift-resistant
     * under concurrent inserts (a row inserted into the middle of the sorted order between
     * two page requests will not produce duplicates or skips at the page boundary).
     * <p>
     * <strong>Sort requirements.</strong> The resolved {@link Sort} must be deterministic,
     * i.e. it must end with a unique key (typically the identifier property). Initial
     * support is limited to a single sort component on the identifier property
     * ({@code +id} / {@code -id}); compound sorts are intentionally rejected until the
     * index audit (recce §5.1) has been completed for the sorting columns of interest.
     * <p>
     * When {@code cursor} is {@code null}, the page returned is the first page in the sort
     * order, with a non-null {@link CursorPage#getNextCursor()} if more rows exist.
     *
     * @param filters filters applied on the search; same shape as
     *                {@link #loadValueObjects(Filters, Sort, int, int)}
     * @param sort    deterministic sort (must end in a unique key); must not be {@code null}
     *                for cursor mode
     * @param cursor  the cursor token decoded from a previous response, or {@code null} to
     *                fetch the first page
     * @param limit   page size; the implementation fetches one additional row internally to
     *                detect whether a next page exists, but the returned page contains at
     *                most {@code limit} rows
     * @return a {@link CursorPage} with the rows, the {@code nextCursor} (or {@code null} at
     *         the end of the collection), {@code prevCursor} (when known), and a
     *         {@code totalElements} of {@code null} (cursor mode does not count by default)
     * @throws UnsupportedOperationException if the implementation does not support cursor
     *                                       pagination yet (e.g. criteria-based DAOs that
     *                                       have not been migrated)
     * @throws IllegalArgumentException      if the sort is not deterministic or its key
     *                                       shape does not match the cursor's
     */
    default CursorPage<VO> loadValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit ) {
        throw new UnsupportedOperationException( "Cursor-based pagination is not implemented for "
                + getClass().getName() + "." );
    }
}
