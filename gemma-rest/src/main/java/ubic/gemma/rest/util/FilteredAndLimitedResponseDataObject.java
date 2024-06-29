package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import java.util.List;

@Getter
public class FilteredAndLimitedResponseDataObject<T> extends FilteredResponseDataObject<T> {

    private final Integer limit;

    public FilteredAndLimitedResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, Integer limit ) {
        super( payload, filters, groupBy, sort );
        this.limit = limit;
    }
}
