package ubic.gemma.rest.util;

import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represent limit results.
 *
 * @param <T>
 */
public class LimitedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    private final String filter;
    private final String[] groupBy;
    private final SortValueObject sort;
    private final int limit;

    public LimitedResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, int limit ) {
        super( payload );
        this.filter = filters != null ? filters.toOriginalString() : null;
        this.groupBy = groupBy;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
        this.limit = limit;
    }

    public String getFilter() {
        return filter;
    }

    public String[] getGroupBy() {
        return groupBy;
    }

    public SortValueObject getSort() {
        return sort;
    }

    public int getLimit() {
        return this.limit;
    }
}
