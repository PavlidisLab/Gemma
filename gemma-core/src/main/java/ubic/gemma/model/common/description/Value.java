package ubic.gemma.model.common.description;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a value.
 * <p>
 * This is used as an immutable container for the value of a {@link Characteristic}.
 *
 * @author poirigui
 */
@Getter
public class Value {

    private final String value;
    @Nullable
    private final String valueUri;

    public Value( String value, @Nullable String valueUri ) {
        if ( value == null ) {
            throw new IllegalArgumentException( "A value must have a label." );
        }
        this.value = value;
        this.valueUri = valueUri;
    }

    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Value ) ) {
            return false;
        }
        Value other = ( Value ) object;
        return CharacteristicUtils.equals( value, valueUri, other.value, other.valueUri );
    }

    public int hashCode() {
        return Objects.hash( StringUtils.lowerCase( valueUri != null ? valueUri : value ) );
    }

    public String toString() {
        StringBuilder b = new StringBuilder( "Value" );
        b.append( " " ).append( "Value=" ).append( value );
        if ( valueUri != null ) {
            b.append( " [" ).append( valueUri ).append( "]" );
        }
        return b.toString();
    }
}
