package ubic.gemma.core.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class GzipUtilsTest {

    private static byte[] expressionLikeRows() {
        Random random = new Random( 1L );
        StringBuilder tsv = new StringBuilder();
        for ( int i = 0; i < 5000; i++ ) {
            tsv.append( "probe_" ).append( i );
            for ( int j = 0; j < 20; j++ ) {
                tsv.append( '\t' ).append( String.format( "%.4f", random.nextGaussian() * 2 + 8 ) );
            }
            tsv.append( '\n' );
        }
        return tsv.toString().getBytes( StandardCharsets.UTF_8 );
    }

    private static byte[] gzip( byte[] payload, int level ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try ( OutputStream os = new GZIPOutputStream( out ) {
            {
                def.setLevel( level );
            }
        } ) {
            os.write( payload );
        }
        return out.toByteArray();
    }

    @Test
    public void compressesAtLevelFour() throws IOException {
        byte[] payload = expressionLikeRows();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try ( OutputStream os = GzipUtils.newGzipOutputStream( out ) ) {
            os.write( payload );
        }
        assertThat( out.toByteArray() )
                .isEqualTo( gzip( payload, 4 ) )
                .isNotEqualTo( gzip( payload, 6 ) );
        try ( GZIPInputStream in = new GZIPInputStream( new ByteArrayInputStream( out.toByteArray() ) ) ) {
            assertThat( in.readAllBytes() ).isEqualTo( payload );
        }
    }
}
