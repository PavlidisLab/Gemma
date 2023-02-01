package ubic.gemma.rest.util;

import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represent limit results.
 * @param <T>
 */
public class LimitedResponseDataObject<T> extends FilteringResponseDataObject<T> {

    private final int limit;

    public LimitedResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, int limit ) {
        super( payload, filters, groupBy, sort );
        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }
}
