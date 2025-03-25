package ubic.gemma.core.util;

public class ShellUtils {

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
