package ubic.gemma.rest.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents the result of a filtering.
 * @param <T>
 * @see ubic.gemma.persistence.service.FilteringVoEnabledService#loadValueObjects(Filters, Sort)
 */
public class FilteringResponseDataObject<T> extends ResponseDataObject<List<T>> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String filter;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String[] groupBy;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     * @param groupBy the fields by which results are grouped
     */
    public FilteringResponseDataObject( List<T> payload, @Nullable Filters filters, @Nullable String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.filter = filters != null ? filters.toOriginalString() : null;
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
