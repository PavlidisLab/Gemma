package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

/**
 * Represents a payload with a limited number of results.
 *
 * @author poirigui
 */
@Getter
public class QueriedAndFilteredAndLimitedResponseDataObject<T> extends FilteredAndLimitedResponseDataObject<T> {

    private final String query;

    public QueriedAndFilteredAndLimitedResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, @Nullable Integer limit ) {
        super( payload, filters, groupBy, sort, limit );
        this.query = query;
    }
}
