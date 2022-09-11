package ubic.gemma.rest.util;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {

    /**
     * Summarize a {@link HttpServletRequest} in a single line format.
     *
     * This is meant to be used for logging or displaying HTTP requests.
     */
    public static String summarizeRequest( HttpServletRequest request ) {
        StringBuilder sb = new StringBuilder();
        sb.append( request.getMethod() ).append( ' ' );
        if ( request.getUserPrincipal() != null ) {
            sb.append( request.getUserPrincipal().getName() ).append( '@' );
        }
        sb.append( request.getRequestURI() );
        if ( request.getQueryString() != null ) {
            sb.append( '?' ).append( request.getQueryString() );
        }
        return sb.toString();
    }
}
