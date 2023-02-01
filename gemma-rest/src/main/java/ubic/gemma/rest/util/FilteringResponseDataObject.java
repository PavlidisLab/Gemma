package ubic.gemma.rest.util;

import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents the result of a filtering.
 * @param <T>
 * @see ubic.gemma.persistence.service.FilteringVoEnabledService#loadValueObjectsPreFilter(Filters, Sort)
 */
public class FilteringResponseDataObject<T> extends ResponseDataObject<List<T>> {

    private final String filter;
    private final String[] groupBy;
    private final SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     * @param groupBy the fields by which results are grouped
     */
    public FilteringResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.filter = filters != null ? filters.toString() : null;
        this.groupBy = groupBy;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
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
}
