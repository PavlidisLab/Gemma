package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @see Responders#all
 */
@Getter
public class FilteredResponseDataObjectImpl<T> extends ResponseDataObjectImpl<List<T>> implements FilteredResponseDataObject<List<T>> {

    private final String filter;
    private final String[] groupBy;
    private final SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteredResponseDataObjectImpl( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.filter = filters != null ? filters.toOriginalString() : null;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
        this.groupBy = groupBy;
    }
}
