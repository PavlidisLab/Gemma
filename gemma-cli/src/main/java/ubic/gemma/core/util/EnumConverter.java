package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convert {@link Enum} to string.
 * <p>
 * This converter accepts both snake-case and kebab-case strings. It will first attempt to match the string as-is, then
 * it will perform an upper case match.
 * @author poirigui
 */
public class EnumConverter<T extends Enum<T>> implements EnumeratedConverter<Enum<T>, IllegalArgumentException> {

    public static <T extends Enum<T>> EnumConverter<T> of( Class<T> enumClass ) {
        return new EnumConverter<>( enumClass, new EnumMap<>( enumClass ) );
    }

    public static <T extends Enum<T>> EnumConverter<T> of( Class<T> enumClass, EnumMap<T, String> descriptions ) {
        return new EnumConverter<>( enumClass, descriptions );
    }

    private final Class<T> enumClass;
    private final EnumMap<T, String> descriptions;
    private final TreeMap<String, Enum<T>> enumByName = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

    private EnumConverter( Class<T> enumClass, EnumMap<T, String> descriptions ) {
        this.enumClass = enumClass;
        this.descriptions = descriptions;
        for ( T e : EnumSet.allOf( enumClass ) ) {
            enumByName.put( e.name(), e );
        }
    }

    @Override
    public Enum<T> apply( String value ) {
        value = StringUtils.strip( value ).replace( '-', '_' );
        try {
            // as-is
            return Enum.valueOf( enumClass, value );
        } catch ( IllegalArgumentException e ) {
            Enum<T> v = enumByName.get( value );
            if ( v == null ) {
                throw e;
            }
            return v;
        }
    }

    @Override
    public Map<String, String> getPossibleValues() {
        return EnumSet.allOf( enumClass ).stream()
                .collect( Collectors.toMap( Enum::name, e -> descriptions.getOrDefault( e, "" ), ( a, b ) -> a, LinkedHashMap::new ) );
    }
}
