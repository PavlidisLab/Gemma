package ubic.gemma.rest.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.List;

/**
 * Represents a payload with a limited number of results.
 *
 * @author poirigui
 */
@Getter
public class QueriedAndFilteredAndLimitedResponseDataObject<T> extends FilteredAndLimitedResponseDataObject<T> {

    @Schema(description = "The full-text query that was applied, echoed back. Null when the request gave none.")
    private final String query;

    public QueriedAndFilteredAndLimitedResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, @Nullable Integer limit ) {
        super( payload, filters, groupBy, sort, limit );
        this.query = query;
    }
}
