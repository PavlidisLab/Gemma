package ubic.gemma.core.util.test;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

/**
 * Reusable assumptions for tests.
 * @author poirigui
 */
public class Assumptions {

    /**
     * Assume that a resource identified by a URL is available.
     * <p>
     * The assumption comprise the following tests:
     * <ul>
     * <li>that the name can be resolved (this no {@link UnknownHostException} is raised</li>
     * <li>that the error code is in the 100, 200 or 300 family (for HTTP URLs)</li>
     * <li>that the connection can be established in 1 second or less</li>
     * </ul>
     * Only a connection is established; the resource itself is not consumed.
     */
    public static void assumeThatResourceIsAvailable( String url ) {
        URLConnection con = null;
        try {
            con = new URL( url ).openConnection();
            // set a very strict connection timeout
            con.setConnectTimeout( 1000 );
            // needed to explicitly attempt a connection since we're not consuming the payload
            con.connect();
            if ( con instanceof HttpURLConnection ) {
                HttpURLConnection httpCon = ( HttpURLConnection ) con;
                assumeTrue( String.format( "The resource at %s responded with a %d %s HTTP status code.",
                                url, httpCon.getResponseCode(), httpCon.getResponseMessage() ),
                        httpCon.getResponseCode() < 500 );
            }
        } catch ( UnknownHostException | ConnectException e ) {
            assumeNoException( String.format( "The resource at %s is not available.", url ), e );
        } catch ( SSLException e ) {
            assumeNoException( String.format( "SSL issue attempting to connect to %s.", url ), e );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        } finally {
            if ( con instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) con ).disconnect();
            }
        }
    }

    public static void assumeThatExceptionIsDueToNetworkIssue( Exception e ) {
        if ( e instanceof IOException ) {
            checkIOException( ( IOException ) e );
        }
        if ( e.getCause() instanceof IOException ) {
            assumeThatExceptionIsDueToNetworkIssue( ( IOException ) e.getCause() );
        }
        throw new RuntimeException( e );
    }

    /**
     * Matches {@link IOException} due to an HTTP 5xx errors.
     */
    private static final Pattern IO_EXCEPTION_MESSAGE_WITH_HTTP_500_ERROR = Pattern.compile( "^Server returned HTTP response code: (5\\d\\d) for URL: (.+)$" );

    private static void checkIOException( IOException e ) {
        if ( e instanceof ConnectException ) {
            assumeNoException( "Test skipped due to connection exception", e );
        } else if ( e instanceof UnknownHostException ) {
            assumeNoException( "Test skipped due to unknown host exception", e );
        } else if ( e instanceof SSLException ) {
            assumeNoException( "SSL issue attempting to connect.", e );
        } else if ( e.getMessage() != null ) {
            Matcher m = IO_EXCEPTION_MESSAGE_WITH_HTTP_500_ERROR.matcher( e.getMessage() );
            if ( m.matches() ) {
                assumeNoException( "Test skipped due to a " + m.group( 1 ) + " error for URL " + m.group( 2 ), e );
            }
        }
    }
}
