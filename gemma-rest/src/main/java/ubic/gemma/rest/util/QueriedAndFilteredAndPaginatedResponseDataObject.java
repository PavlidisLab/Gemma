package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;

@Getter
public class QueriedAndFilteredAndPaginatedResponseDataObject<T> extends FilteredAndPaginatedResponseDataObject<T> {

    private final String query;

    public QueriedAndFilteredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy ) {
        super( payload, filters, groupBy );
        this.query = query;
    }
}
