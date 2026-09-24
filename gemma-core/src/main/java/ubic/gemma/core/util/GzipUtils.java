package ubic.gemma.core.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * gzip at the deflate level Gemma uses for what it generates: data files, their in-band streams and REST responses.
 * <p>
 * The level is 4, not zlib's default of 6. Measured with zlib on payloads taken from production, 2026-09-18:
 * <pre>
 *                                                     level 6            level 4
 * datasets/MSSM_Cohort/singleCellDimension  186 MB    12.0 s  23.1 MB    1.9 s  24.2 MB
 * datasets/GSE19804/data/processed           49 MB     4.5 s  19.2 MB    1.3 s  20.8 MB
 *   singleCellDimension?exclude=cellIds      63 MB     1.2 s   2.7 MB    0.3 s   3.2 MB
 * </pre>
 * Expression data and cell ids compress poorly at any level, so level 6 buys a few percent for three to six times
 * the time, and that time is paid by whoever is waiting for the file or the response. Paul, 2026-09-18: level 4.
 * The cell ids are the worst case: high-entropy strings sharing long prefixes ({@code Donor_1-2-AGAGAATGTTAATCGC-1})
 * defeat the long match chains of levels 5 and up. Levels 1 and 2 are another 0.5 s faster on the first payload but
 * 21 to 43% larger than level 4 on smaller, more compressible ones; level 3 is slower than 4.
 * <p>
 * Not used for the database blob behind {@code CompressedStringListType}, nor for files the GEO loader writes.
 */
public final class GzipUtils {

    public static final int LEVEL = 4;

    private static final int BUFFER_SIZE = 8192;

    private GzipUtils() {
    }

    /**
     * A {@link GZIPOutputStream} over the given stream, at {@link #LEVEL}.
     */
    public static GZIPOutputStream newGzipOutputStream( OutputStream out ) throws IOException {
        return new GZIPOutputStream( out, BUFFER_SIZE ) {
            {
                def.setLevel( LEVEL );
            }
        };
    }
}
