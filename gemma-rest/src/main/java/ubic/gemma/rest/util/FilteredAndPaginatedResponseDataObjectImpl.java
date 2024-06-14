package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public class FilteredAndPaginatedResponseDataObjectImpl<T> extends PaginatedResponseDataObjectImpl<T> implements FilteredAndPaginatedResponseDataObject<List<T>> {

    private final String filter;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteredAndPaginatedResponseDataObjectImpl( Slice<T> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
        super( payload, groupBy );
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}