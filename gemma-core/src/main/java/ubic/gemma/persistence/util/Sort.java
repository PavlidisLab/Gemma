package ubic.gemma.persistence.util;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a directed sort by a property.
 */
@Getter
@EqualsAndHashCode
@ToString
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

    private final String orderBy;
    private final Direction direction;

    public Sort( String orderBy, Direction direction ) {
        if ( StringUtils.isBlank( orderBy ) ) {
            throw new IllegalArgumentException( "The 'orderBy' property cannot be null or empty." );
        }
        this.orderBy = orderBy;
        this.direction = direction;
    }
}
