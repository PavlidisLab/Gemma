package ubic.gemma.rest.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import org.springframework.lang.Nullable;

@Getter
public class FilteredAndPaginatedResponseDataObject<T> extends PaginatedResponseDataObject<T> {

    @Schema(description = "The filter that was applied, echoed back as it was parsed. Null when the request gave none.")
    private final String filter;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    public FilteredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
        super( payload, groupBy );
        this.filter = filters != null ? filters.toOriginalString() : null;
    }
}