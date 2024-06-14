package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public class QueriedAndFilteredAndPaginatedResponseDataObjectImpl<T> extends FilteredAndPaginatedResponseDataObjectImpl<T> implements QueriedAndFilteredAndPaginatedResponseDataObject<List<T>> {

    private final String query;

    public QueriedAndFilteredAndPaginatedResponseDataObjectImpl( Slice<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy ) {
        super( payload, filters, groupBy );
        this.query = query;
    }
}
