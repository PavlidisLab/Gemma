package ubic.gemma.core.util;

import java.net.URL;

public class ResourceUtils {

    /**
     * Attempt to resolve a likely source code location for a given resource.
     */
    public static String getSourceCodeLocation( URL resourceUrl ) {
        if ( resourceUrl.getProtocol().equals( "file" ) ) {
            String s = resourceUrl
                    .toString()
                    .replaceFirst( "^file:", "file://" );
            if ( s.endsWith( ".class" ) ) {
                return s
                        .replaceFirst( "\\.class$", ".java" )
                        .replace( "target/classes", "src/main/java" );
            } else {
                return s.replace( "target/classes", "src/main/resources" );
            }
        } else {
            return resourceUrl.toString();
        }
    }
}
