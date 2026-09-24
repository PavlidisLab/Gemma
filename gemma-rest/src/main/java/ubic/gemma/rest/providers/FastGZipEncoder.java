package ubic.gemma.rest.providers;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import org.glassfish.jersey.message.GZipEncoder;
import ubic.gemma.core.util.GzipUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Jersey's {@link GZipEncoder}, compressing at {@link GzipUtils#LEVEL} instead of zlib's default of 6.
 * <p>
 * Every gzipped response is compressed on the fly, so the level is paid in response time.
 * {@code datasets/MSSM_Cohort/singleCellDimension} took 18.5 s on production, of which about 15 s was spent after
 * the first byte compressing 186 MB of JSON at level 6. {@link GzipUtils} has the measurements behind the level.
 * <p>
 * Registered by class name in {@code web.xml} in place of {@link GZipEncoder}, and deliberately not a
 * {@code @Provider}: the package scan would register it beside Jersey's, and two encoders would claim {@code gzip}.
 */
@Priority(Priorities.ENTITY_CODER)
public class FastGZipEncoder extends GZipEncoder {

    @Override
    public OutputStream encode( String contentEncoding, OutputStream entityStream ) throws IOException {
        return GzipUtils.newGzipOutputStream( entityStream );
    }
}
