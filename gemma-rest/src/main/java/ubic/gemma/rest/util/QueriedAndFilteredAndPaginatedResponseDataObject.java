package ubic.gemma.rest.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class QueriedAndFilteredAndPaginatedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    String query;
    String filter;
    String[] groupBy;
    SortValueObject sort;
    Integer offset;
    Integer limit;
    Long totalElements;

    public QueriedAndFilteredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy ) {
        super( payload );
        this.query = query;
        this.offset = payload.getOffset();
        this.limit = payload.getLimit();
        this.totalElements = payload.getTotalElements();
        this.sort = payload.getSort() != null ? new SortValueObject( payload.getSort() ) : null;
        this.groupBy = groupBy;
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}
