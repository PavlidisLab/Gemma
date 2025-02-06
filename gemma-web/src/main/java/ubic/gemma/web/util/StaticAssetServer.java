package ubic.gemma.web.util;

public interface StaticAssetServer {

    String getBaseUrl();

    boolean isAlive();

    /**
     * Obtain launch instructions if the server is not running.
     */
    String getLaunchInstruction();
}
