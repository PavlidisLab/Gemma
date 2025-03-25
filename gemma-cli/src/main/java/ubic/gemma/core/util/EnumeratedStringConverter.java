package ubic.gemma.core.util;

import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A string converter that can only convert from a finite set of possible values.
 * @author poirigui
 */
public class EnumeratedStringConverter implements EnumeratedConverter<String, IllegalArgumentException> {

    public static EnumeratedStringConverter of( String... possibleValues ) {
        return new EnumeratedStringConverter( Arrays.stream( possibleValues )
                .collect( Collectors.toMap( e -> e, e -> "", ( a, b ) -> a, TreeMap::new ) ) );
    }

    public static EnumeratedStringConverter of( Map<String, String> descriptions ) {
        return new EnumeratedStringConverter(  descriptions  );
    }

    private final Map<String, String> possibleValues;

    private EnumeratedStringConverter( Map<String, String> possibleValues ) {
        this.possibleValues = possibleValues;
    }

    @Override
    public Map<String, String> getPossibleValues() {
        return possibleValues;
    }

    @Override
    public String apply( String string ) throws IllegalArgumentException {
        if ( !possibleValues.containsKey( string ) ) {
            throw new IllegalArgumentException( "Unsupported value: " + string );
        }
        return string;
    }
}
