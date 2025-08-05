package ubic.gemma.core.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ShellUtils {

    public static String join( String... args ) {
        return Arrays.stream( args )
                .map( ShellUtils::quoteIfNecessary )
                .collect( Collectors.joining( " " ) );
    }

    public static String quoteIfNecessary( String s ) {
        if ( s.contains( "'" ) || s.contains( " " ) || s.contains( "\t" ) || s.contains( "\n" ) ) {
            return "'" + s.replaceAll( "'", "'\"'\"'" )
                    .replaceAll( "\t", "\\\\t" )
                    .replaceAll( "\n", "\\\\n" )
                    + "'";
        } else {
            return s;
        }
    }
}
