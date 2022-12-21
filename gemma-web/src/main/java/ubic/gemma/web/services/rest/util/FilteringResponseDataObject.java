package ubic.gemma.web.services.rest.util;

import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

public class FilteringResponseDataObject<T> extends ResponseDataObject<List<T>> {

    private final String filter;
    private final SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteringResponseDataObject( List<T> payload, @Nullable Filters filters, @Nullable Sort sort ) {
        super( payload );
        this.filter = filters != null ? filters.toString() : null;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
    }

    public String getFilter() {
        return filter;
    }

    public SortValueObject getSort() {
        return sort;
    }
}
