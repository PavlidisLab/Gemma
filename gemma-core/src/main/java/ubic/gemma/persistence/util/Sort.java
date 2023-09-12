package ubic.gemma.persistence.util;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;

/**
 * Represents a directed sort by a property.
 */
@Value
public class Sort implements PropertyMapping {

    /**
     * Create a {@link Sort} for a given alias, property and direction.
     *
     * @param alias            an alias in the query, or null to refer to the root entity (not recommended though, since this
     *                         could result in an ambiguous query)
     * @param propertyName     a property of objectAlias to order by
     * @param direction        a direction, or null for default
     * @param originalProperty
     */
    public static Sort by( @Nullable String alias, String propertyName, @Nullable Direction direction, String originalProperty ) {
        return new Sort( alias, propertyName, direction, originalProperty );
    }

    /**
     * Create a sort without an original property.
     * @see #by(String, String, Direction)
     */
    public static Sort by( @Nullable String alias, String propertyName, @Nullable Direction direction ) {
        return new Sort( alias, propertyName, direction, null );
    }

    /**
     * Direction of the sort.
     */
    public enum Direction {
        ASC, DESC;

        @Override
        public String toString() {
            return this == ASC ? "+" : "-";
        }
    }

    @Nullable
    String objectAlias;
    String propertyName;
    @Nullable
    Direction direction;
    /**
     * Indicate the original property from which this sort originates.
     */
    @Nullable
    String originalProperty;

    private Sort( @Nullable String objectAlias, String propertyName, @Nullable Direction direction, @Nullable String originalProperty ) {
        if ( objectAlias != null && StringUtils.isBlank( objectAlias ) ) {
            throw new IllegalArgumentException( "The object alias must be either null or non-empty." );
        }
        if ( StringUtils.isBlank( propertyName ) ) {
            throw new IllegalArgumentException( "The property name cannot be null or empty." );
        }
        this.objectAlias = objectAlias;
        this.propertyName = propertyName;
        this.direction = direction;
        this.originalProperty = originalProperty;
    }

    @Override
    public String toString() {
        return toString( false );
    }

    @Override
    public String toOriginalString() {
        return toString( true );
    }

    private String toString( boolean withOriginalProperties ) {
        return String.format( "%s%s",
                direction != null ? direction.toString() : "",
                withOriginalProperties ? originalProperty : formProperty( this ) );
    }
}
