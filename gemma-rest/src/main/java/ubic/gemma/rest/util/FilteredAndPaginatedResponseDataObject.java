package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

@Getter
public class FilteredAndPaginatedResponseDataObject<T> extends PaginatedResponseDataObject<T> {

    private final String filter;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
        super( payload, groupBy );
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}