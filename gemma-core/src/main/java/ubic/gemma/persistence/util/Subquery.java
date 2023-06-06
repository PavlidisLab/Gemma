package ubic.gemma.persistence.util;

import lombok.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a subquery right-hand side of a {@link Filter}.
 * <p>
 * @author poirgui
 */
@Value
public class Subquery {

    @Value
    public static class Alias {
        @Nullable
        String objectAlias;
        String propertyName;
        String alias;
    }

    /**
     * The entity name being queried.
     */
    String entityName;
    /**
     * The property name being queried.
     */
    String propertyName;
    /**
     * List of aliases for resolving the object alias defined in {@link #filter}.
     */
    List<Alias> aliases;
    /**
     * A filter being nested in the subquery.
     */
    Filter filter;
}
