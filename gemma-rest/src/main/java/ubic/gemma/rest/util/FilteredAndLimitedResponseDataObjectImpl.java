package ubic.gemma.rest.util;

import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public class FilteredAndLimitedResponseDataObjectImpl<T> extends FilteredResponseDataObjectImpl<T> implements FilteredAndLimitedResponseDataObject<List<T>> {

    private final Integer limit;

    public FilteredAndLimitedResponseDataObjectImpl( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, Integer limit ) {
        super( payload, filters, groupBy, sort );
        this.limit = limit;
    }
}
