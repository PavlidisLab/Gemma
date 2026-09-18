package ubic.gemma.rest.providers;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import org.glassfish.jersey.message.GZipEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Jersey's {@link GZipEncoder}, compressing at deflate level 4 instead of zlib's default of 6.
 * <p>
 * Every gzipped response is compressed on the fly, so the level is paid in response time. Measured with zlib on
 * payloads taken from production, 2026-09-18:
 * <pre>
 *                                                     level 6            level 4
 * datasets/MSSM_Cohort/singleCellDimension  186 MB    12.0 s  23.1 MB    1.9 s  24.2 MB
 *   the same, ?exclude=cellIds               63 MB     1.2 s   2.7 MB    0.3 s   3.2 MB
 * datasets/GSE260875/analyses/differential  961 KB    0.01 s  34 KB     0.00 s  44 KB
 * </pre>
 * The first took 18.5 s on production, of which about 15 s was spent after the first byte. Its 3.7 million cell ids
 * are high-entropy strings sharing long prefixes ({@code Donor_1-2-AGAGAATGTTAATCGC-1}), which is the worst case for
 * the long match chains of levels 5 and up. Levels 1 and 2 are another 0.5 s faster on it, but 21 to 43% larger
 * than level 4 on the other two; level 3 is slower than 4.
 * <p>
 * Registered by class name in {@code web.xml} in place of {@link GZipEncoder}, and deliberately not a
 * {@code @Provider}: the package scan would register it beside Jersey's, and two encoders would claim {@code gzip}.
 */
@Priority(Priorities.ENTITY_CODER)
public class FastGZipEncoder extends GZipEncoder {

    static final int LEVEL = 4;

    private static final int BUFFER_SIZE = 8192;

    @Override
    public OutputStream encode( String contentEncoding, OutputStream entityStream ) throws IOException {
        return new GZIPOutputStream( entityStream, BUFFER_SIZE ) {
            {
                def.setLevel( LEVEL );
            }
        };
    }
}
