package ubic.gemma.rest.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.List;

/**
 * @see Responders#all
 */
@Getter
public class FilteredResponseDataObject<T> extends ResponseDataObject<List<T>> {

    @Schema(description = "The filter that was applied, echoed back as it was parsed. Null when the request gave none.")
    private final String filter;
    @Schema(description = "The properties the results are grouped by.")
    private final String[] groupBy;
    @Schema(description = "How the results are ordered.")
    private final SortValueObject sort;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteredResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload );
        this.filter = filters != null ? filters.toOriginalString() : null;
        this.sort = sort != null ? new SortValueObject( sort ) : null;
        this.groupBy = groupBy;
    }
}
