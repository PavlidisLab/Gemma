package ubic.gemma.rest.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class GroupedResponseDataObject<T> extends ResponseDataObject<List<T>> {

    String[] groupBy;
    SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public GroupedResponseDataObject( List<T> payload, String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.groupBy = groupBy;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
    }
}
