package ubic.gemma.web.services.rest.util;

import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

public class PaginatedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    private final Integer offset;

    private final Integer limit;

    private final Sort sort;

    private final Long totalElements;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    PaginatedResponseDataObject( Slice<T> payload ) {
        super( payload );
        this.offset = payload.getOffset();
        this.limit = payload.getLimit();
        this.sort = payload.getSort();
        this.totalElements = payload.getTotalElements();
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public Sort getSort() {
        return sort;
    }

    public Long getTotalElements() {
        return totalElements;
    }
}
