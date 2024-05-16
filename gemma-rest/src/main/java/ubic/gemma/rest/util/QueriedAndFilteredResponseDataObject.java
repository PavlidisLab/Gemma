package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public class QueriedAndFilteredResponseDataObject<T> extends FilteredResponseDataObject<T> {

    private final String query;

    public QueriedAndFilteredResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload, filters, groupBy, sort );
        this.query = query;
    }
}
