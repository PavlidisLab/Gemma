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

    String entityName;
    String identifierPropertyName;
    List<Alias> aliases;
    Filter filter;
}
