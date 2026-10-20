package ubic.gemma.rest.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.List;

@Getter
public class FilteredAndLimitedResponseDataObject<T> extends FilteredResponseDataObject<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The cap that was applied to the number of results.")
    private final Integer limit;

    public FilteredAndLimitedResponseDataObject( List<T> payload, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, Integer limit ) {
        super( payload, filters, groupBy, sort );
        this.limit = limit;
    }
}
