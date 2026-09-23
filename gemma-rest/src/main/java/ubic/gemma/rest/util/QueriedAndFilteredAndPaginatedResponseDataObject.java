package ubic.gemma.rest.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import org.springframework.lang.Nullable;

@Getter
public class QueriedAndFilteredAndPaginatedResponseDataObject<T> extends FilteredAndPaginatedResponseDataObject<T> {

    @Schema(description = "The full-text query that was applied, echoed back. Null when the request gave none.")
    private final String query;

    public QueriedAndFilteredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy ) {
        super( payload, filters, groupBy );
        this.query = query;
    }
}
