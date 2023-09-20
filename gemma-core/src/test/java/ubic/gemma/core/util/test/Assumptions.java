package ubic.gemma.core.util.test;

import javax.net.ssl.SSLException;
import java.net.*;

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
    public static void assumeThatResourceIsAvailable( String url ) throws Exception {
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
                        httpCon.getResponseCode() < 400 );
            }
        } catch ( UnknownHostException | ConnectException e ) {
            assumeNoException( String.format( "The resource at %s is not available.", url ), e );
        } catch ( SSLException e ) {
            assumeNoException( String.format( "SSL issue attempting to connect to %s.", url ), e );
        } finally {
            if ( con instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) con ).disconnect();
            }
        }
    }
}
