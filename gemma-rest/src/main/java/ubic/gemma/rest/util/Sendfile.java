package ubic.gemma.rest.util;

import lombok.Value;

import java.nio.file.Path;

/**
 * Indicate that a file should be sent using <a href="https://tomcat.apache.org/tomcat-9.0-doc/aio.html">Tomcat sendfile</a>.
 * <p>
 * Note that if sendfile is not supported by the server, this will fall back to a regular file download.
 * @author poirigui
 */
@Value(staticConstructor = "of")
public class Sendfile {
    /**
     * File path to send.
     */
    Path path;
}
