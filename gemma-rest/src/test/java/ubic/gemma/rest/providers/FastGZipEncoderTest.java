package ubic.gemma.rest.providers;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FastGZipEncoderTest {

    /**
     * Cell ids shaped like MSSM_Cohort's, the payload the level was chosen for.
     */
    private static byte[] cellIds() {
        Random random = new Random( 1L );
        String bases = "ACGT";
        StringBuilder json = new StringBuilder( "[" );
        for ( int i = 0; i < 20000; i++ ) {
            json.append( "\"Donor_" ).append( 1 + random.nextInt( 40 ) ).append( '-' ).append( 1 + random.nextInt( 3 ) ).append( '-' );
            for ( int j = 0; j < 16; j++ ) {
                json.append( bases.charAt( random.nextInt( 4 ) ) );
            }
            json.append( "-" ).append( random.nextInt( 2 ) ).append( "\"," );
        }
        return json.append( "\"\"]" ).toString().getBytes( StandardCharsets.UTF_8 );
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
        byte[] payload = cellIds();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try ( OutputStream os = new FastGZipEncoder().encode( "gzip", out ) ) {
            os.write( payload );
        }
        assertThat( out.toByteArray() )
                .isEqualTo( gzip( payload, 4 ) )
                .isNotEqualTo( gzip( payload, 6 ) );
        try ( GZIPInputStream in = new GZIPInputStream( new ByteArrayInputStream( out.toByteArray() ) ) ) {
            assertThat( in.readAllBytes() ).isEqualTo( payload );
        }
    }

    /**
     * The production servlet registers its encoder by class name, so nothing else would notice it reverting.
     */
    @Test
    public void webXmlRegistersThisEncoderAndNotJerseys() throws IOException {
        String webXml = Files.readString( Paths.get( "src/main/webapp/WEB-INF/web.xml" ) );
        assertThat( webXml )
                .contains( FastGZipEncoder.class.getName() )
                .doesNotContain( "org.glassfish.jersey.message.GZipEncoder" );
    }
}
