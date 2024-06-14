package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public class QueriedAndFilteredAndLimitedResponseDataObjectImpl<T> extends FilteredAndLimitedResponseDataObjectImpl<T> implements QueriedAndFilteredAndLimitedResponseDataObject<List<T>> {

    private final String query;

    public QueriedAndFilteredAndLimitedResponseDataObjectImpl( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, @Nullable Integer limit ) {
        super( payload, filters, groupBy, sort, limit );
        this.query = query;
    }
}
