package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

/**
 * @see Responders#all
 */
@Getter
public class FilteredResponseDataObject<T> extends ResponseDataObject<List<T>> {

    private final String filter;
    private final String[] groupBy;
    private final SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteredResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.filter = filters != null ? filters.toOriginalString() : null;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
        this.groupBy = groupBy;
    }
}
