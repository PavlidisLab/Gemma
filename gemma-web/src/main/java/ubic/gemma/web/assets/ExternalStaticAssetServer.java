package ubic.gemma.web.assets;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * An external static asset server that provide assets from a specified base URL.
 * @author poirigui
 */
public class ExternalStaticAssetServer implements StaticAssetServer {

    private final String baseUrl;

    public ExternalStaticAssetServer( String baseUrl ) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public boolean isAlive() {
        URLConnection connection = null;
        try {
            connection = new URL( baseUrl ).openConnection();
            connection.connect();
            return true;
        } catch ( ConnectException e ) {
            return false;
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to access the static asset server.", e );
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
    }

    @Override
    public String getLaunchInstruction() {
        return "You may start it with:<br>"
                + "<code>npm --prefix gemma-web/src/main/webapp run serve</code><br>"
                + "or by launching the \" Serve static assets \" run configuration in IntelliJ.";
    }
}
