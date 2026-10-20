package ubic.gemma.rest.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.List;

@Getter
public class QueriedAndFilteredResponseDataObject<T> extends FilteredResponseDataObject<T> {

    @Schema(description = "The full-text query that was applied, echoed back. Null when the request gave none.")
    private final String query;

    public QueriedAndFilteredResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort ) {
        super( payload, filters, groupBy, sort );
        this.query = query;
    }
}
