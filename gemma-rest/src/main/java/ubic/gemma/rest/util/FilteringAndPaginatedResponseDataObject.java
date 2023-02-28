package ubic.gemma.rest.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class FilteringAndPaginatedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    String filter;
    String[] groupBy;
    SortValueObject sort;
    Integer offset;
    Integer limit;
    Long totalElements;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteringAndPaginatedResponseDataObject( Slice<T> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
        super( payload );
        this.offset = payload.getOffset();
        this.limit = payload.getLimit();
        this.totalElements = payload.getTotalElements();
        this.sort = payload.getSort() != null ? new SortValueObject( payload.getSort() ) : null;
        this.groupBy = groupBy;
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}