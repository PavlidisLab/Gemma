package ubic.gemma.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * @author poirigui
 */
public class FileUtils {

    /**
     * Open a GZIP-compressed file safely.
     * <p>
     * This ensures that if an exception is thrown by {@link GZIPInputStream#GZIPInputStream(InputStream)}, the stream
     * will be closed.
     */
    public static InputStream openCompressedFile( Path path ) throws IOException {
        InputStream is = Files.newInputStream( path );
        try {
            return new GZIPInputStream( is );
        } catch ( Exception e ) {
            is.close();
            throw e;
        }
    }
}
