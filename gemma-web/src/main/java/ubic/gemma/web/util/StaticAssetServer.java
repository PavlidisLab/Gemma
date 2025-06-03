package ubic.gemma.web.util;

/**
 * @author poirigui
 */
public interface StaticAssetServer {

    /**
     * Obtain the base URL for resolving static assets from the server.
     */
    String getBaseUrl();

    /**
     * Indicate if the static asset server is running.
     */
    boolean isAlive();

    /**
     * Obtain launch instructions if the server is not running.
     */
    @SuppressWarnings("unused") /* see header.jsp for usage */
    String getLaunchInstruction();
}
