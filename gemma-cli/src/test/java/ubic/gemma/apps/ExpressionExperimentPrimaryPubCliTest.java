package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionExperimentPrimaryPubCliTest {

    /**
     * The file was registered as {@code -pubmedIDFile} and read as {@code pmidFile}, so it was never loaded and every
     * dataset fell back to GEO's link.
     */
    @Test
    public void testPubmedIdFileIsRead( @TempDir Path tempDir ) throws Exception {
        Path pubmedIdFile = tempDir.resolve( "pmids.tsv" );
        Files.write( pubmedIdFile, Collections.singletonList( "22438826\tGSE27715" ), StandardCharsets.UTF_8 );
        ExpressionExperimentPrimaryPubCli cli = new ExpressionExperimentPrimaryPubCli();
        CommandLine commandLine = new DefaultParser()
                .parse( cli.getOptions(), new String[] { "-e", "GSE27715", "-pubmedIDFile", pubmedIdFile.toString() } );
        cli.processExperimentOptions( commandLine );
        assertThat( ReflectionTestUtils.getField( cli, "pubmedIds" ) )
                .isEqualTo( Collections.singletonMap( "GSE27715", "22438826" ) );
    }
}
