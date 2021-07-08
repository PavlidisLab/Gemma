package ubic.gemma.web.services.rest.util;

import java.util.Arrays;
import java.util.List;

public class StringUtils {

    /**
     * Split a string by the ',' comma character and trim the resulting pieces.
     *
     * This is meant to be used for parsing query arguments that use a comma as a delimiter.
     *
     * @param  arg the string to process
     * @return trimmed strings exploded from the input.
     */
    public static List<String> splitAndTrim( String arg ) {
        String[] array = arg.split( "," );
        for ( int i = 0; i < array.length; i++ )
            array[i] = array[i].trim();
        return Arrays.asList( array );
    }
}
