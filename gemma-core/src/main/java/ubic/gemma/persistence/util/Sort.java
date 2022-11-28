package ubic.gemma.persistence.util;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

/**
 * Represents a directed sort by a property.
 */
@Value
public class Sort {

    /**
     * Create a {@link Sort} for a given alias, property and direction.
     *
     * @param alias        an alias in the query, or null to refer to the root entity (not recommended though, since this
     *                     could result in an ambiguous query)
     * @param propertyName a property of objectAlias to order by
     * @param direction    a direction, or null for default
     */
    public static Sort by( @Nullable String alias, String propertyName, @Nullable Direction direction ) {
        return new Sort( alias, propertyName, direction );
    }

    /**
     * Create a {@link Sort} with the default direction for the property.
     */
    public static Sort by( @Nullable String alias, String propertyName ) {
        return by( alias, propertyName, null );
    }

    /**
     * Direction of the sort.
     */
    public enum Direction {
        ASC, DESC
    }

    @Nullable
    String objectAlias;
    String propertyName;
    @Nullable
    Direction direction;

    private Sort( @Nullable String objectAlias, String propertyName, @Nullable Direction direction ) {
        if ( objectAlias != null && StringUtils.isBlank( objectAlias ) ) {
            throw new IllegalArgumentException( "The object alias must be either null or non-empty." );
        }
        if ( StringUtils.isBlank( propertyName ) ) {
            throw new IllegalArgumentException( "The property name cannot be null or empty." );
        }
        this.objectAlias = objectAlias;
        this.propertyName = propertyName;
        this.direction = direction;
    }
}
