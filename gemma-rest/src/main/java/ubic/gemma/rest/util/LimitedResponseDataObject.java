package ubic.gemma.rest.util;

import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

public class LimitedResponseDataObject<T> extends FilteringResponseDataObject<T> {

    private final int limit;

    public LimitedResponseDataObject( List<T> payload, @Nullable Filters filters, @Nullable Sort sort, int limit ) {
        super( payload, filters, sort );
        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }
}
