package ubic.gemma.rest.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a payload with a limited number of results.
 * @author poirigui
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class LimitedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    String query;
    String filter;
    String[] groupBy;
    SortValueObject sort;
    Integer limit;

    public LimitedResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, @Nullable Integer limit ) {
        super( payload );
        this.query = query;
        this.filter = filters != null ? filters.toOriginalString() : null;
        this.groupBy = groupBy;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
        this.limit = limit;
    }
}
