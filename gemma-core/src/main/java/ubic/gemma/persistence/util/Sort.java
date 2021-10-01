package ubic.gemma.persistence.util;

import lombok.*;

/**
 * Represents a directed sort by a property.
 */
@Data
public class Sort {

    /**
     * Factory to create a {@link Sort} elegantly.
     * @see Sort#Sort(String, Direction)
     */
    public static Sort by( String orderBy, Direction direction ) {
        return new Sort( orderBy, direction );
    }

    /**
     * Shortcut to provide orderBy without passing a null direction to {@link #by(String, Direction)}.
     */
    public static Sort by( String orderBy ) {
        return by( orderBy, null );
    }

    /**
     * Direction of the sort.
     */
    public enum Direction {
        ASC, DESC
    }

    @NonNull
    private final String orderBy;
    private final Direction direction;
}
