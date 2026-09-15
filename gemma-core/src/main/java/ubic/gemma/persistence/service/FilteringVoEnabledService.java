package ubic.gemma.persistence.service;

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
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

    /**
     * Keyset (cursor) pagination — service-layer pass-through to
     * {@link FilteringVoEnabledDao#loadValueObjectsByCursor(Filters, Sort, Cursor, int)}.
     *
     * @see FilteringVoEnabledDao#loadValueObjectsByCursor(Filters, Sort, Cursor, int)
     */
    default CursorPage<VO> loadValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit ) {
        throw new UnsupportedOperationException( "Cursor-based pagination is not implemented for "
                + getClass().getName() + "." );
    }
}