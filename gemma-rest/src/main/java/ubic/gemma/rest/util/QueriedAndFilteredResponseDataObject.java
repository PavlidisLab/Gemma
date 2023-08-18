package ubic.gemma.rest.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class QueriedAndFilteredResponseDataObject<T> extends ResponseDataObject<List<T>> {

    String query;
    String filter;
    String[] groupBy;
    String sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public QueriedAndFilteredResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.query = query;
        this.filter = filters != null ? filters.toOriginalString() : null;
        this.sort = sort != null ? sort.toOriginalString() : null;
        this.groupBy = groupBy;
    }
}
