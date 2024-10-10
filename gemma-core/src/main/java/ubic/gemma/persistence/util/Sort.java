package ubic.gemma.persistence.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import static ubic.gemma.persistence.util.PropertyMappingUtils.formProperty;

/**
 * Represents a directed sort by a property.
 */
@Value
@EqualsAndHashCode(of = { "objectAlias", "propertyName", "direction", "andThen" })
public class Sort implements PropertyMapping {

    /**
     * Create a {@link Sort} for a given alias, property and direction.
     *
     * @param alias            an alias in the query, or null to refer to the root entity (not recommended though, since this
     *                         could result in an ambiguous query)
     * @param propertyName     a property of objectAlias to order by
     * @param direction        a direction, or null for default
     * @param originalProperty an original property name for rendering via {@link #toOriginalString()}
     */
    public static Sort by( @Nullable String alias, String propertyName, @Nullable Direction direction, NullMode nullMode, String originalProperty ) {
        return new Sort( alias, propertyName, direction, nullMode, originalProperty, null );
    }

    /**
     * Create a sort without an original property.
     * @see #by(String, String, Direction, NullMode)
     */
    public static Sort by( @Nullable String alias, String propertyName, @Nullable Direction direction, NullMode nullMode ) {
        return new Sort( alias, propertyName, direction, nullMode, null, null );
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

    public enum NullMode {
        DEFAULT,
        FIRST,
        LAST
    }

    @Nullable
    String objectAlias;
    String propertyName;
    @Nullable
    Direction direction;
    /**
     * Indicate if null values should appear last when sorting.
     */
    NullMode nullMode;
    /**
     * Indicate the original property from which this sort originates.
     */
    @Nullable
    String originalProperty;
    @Nullable
    Sort andThen;

    private Sort( @Nullable String objectAlias, String propertyName, @Nullable Direction direction, NullMode nullMode, @Nullable String originalProperty, @Nullable Sort andThen ) {
        if ( objectAlias != null && StringUtils.isBlank( objectAlias ) ) {
            throw new IllegalArgumentException( "The object alias must be either null or non-empty." );
        }
        if ( StringUtils.isBlank( propertyName ) ) {
            throw new IllegalArgumentException( "The property name cannot be null or empty." );
        }
        this.objectAlias = objectAlias;
        this.propertyName = propertyName;
        this.direction = direction;
        this.nullMode = nullMode;
        this.originalProperty = originalProperty;
        this.andThen = andThen;
    }

    /**
     * Add a next sort.
     */
    public Sort andThen( Sort andThen ) {
        for ( Sort b = this; b != null; b = b.andThen ) {
            if ( b == andThen ) {
                throw new IllegalArgumentException( "Creating cycles in the sort chain is not allowed." );
            }
        }
        return new Sort( this.objectAlias, this.propertyName, this.direction, this.nullMode, this.originalProperty, andThen );
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
                withOriginalProperties ? originalProperty : formProperty( this ) )
                + ( andThen != null ? ", " + andThen.toString( withOriginalProperties ) : "" );
    }
}
