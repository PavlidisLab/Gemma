package ubic.gemma.rest.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Filters;

import javax.annotation.Nullable;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class FilteredResponseDataObject<T> extends ResponseDataObject<List<T>> {

    @Nullable
    String filter;

    public FilteredResponseDataObject( List<T> payload, @Nullable Filters filters ) {
        super( payload );
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}
