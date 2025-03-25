package ubic.gemma.core.util;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A string converter that can only convert from a finite set of possible values.
 * @author poirigui
 */
public class EnumeratedStringConverter implements EnumeratedConverter<String, IllegalArgumentException> {

    private static final MessageSourceResolvable EMPTY_MESSAGE = new DefaultMessageSourceResolvable( null, null, "" );

    public static EnumeratedStringConverter of( String... possibleValues ) {
        return new EnumeratedStringConverter( Arrays.stream( possibleValues )
                .collect( Collectors.toMap( e -> e, e -> EMPTY_MESSAGE, ( a, b ) -> a, TreeMap::new ) ) );
    }

    public static EnumeratedStringConverter of( Map<String, MessageSourceResolvable> descriptions ) {
        return new EnumeratedStringConverter( descriptions );
    }

    private final Map<String, MessageSourceResolvable> possibleValues;

    private EnumeratedStringConverter( Map<String, MessageSourceResolvable> possibleValues ) {
        this.possibleValues = possibleValues;
    }

    @Override
    public Map<String, MessageSourceResolvable> getPossibleValues() {
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
