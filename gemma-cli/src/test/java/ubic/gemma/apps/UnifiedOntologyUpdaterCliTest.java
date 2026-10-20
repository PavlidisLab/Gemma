package ubic.gemma.apps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * updateUnifiedOntology -skipDownload builds only from a complete set of source files.
 */
public class UnifiedOntologyUpdaterCliTest {

    /**
     * 🛑 A missing source file is a failure, before any TDB dataset is built. It was a WARN: the dataset
     * was built without that ontology and replaced the live one, and the run exited 0.
     */
    @Test
    public void testAMissingSourceFailsBeforeAnythingIsBuilt( @TempDir Path dir ) throws Exception {
        Path sourcesDir = Files.createDirectory( dir.resolve( "sources" ) );
        // one of the listed sources is present, the rest are not
        Files.write( sourcesDir.resolve( "ro.owl" ), Collections.singletonList(
                "<?xml version=\"1.0\"?>"
                        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\">"
                        + "<owl:Ontology rdf:about=\"http://purl.obolibrary.org/obo/ro.owl\"/></rdf:RDF>" ),
                StandardCharsets.UTF_8 );
        Path destDir = dir.resolve( "tdb" );

        UnifiedOntologyUpdaterCli cli = new UnifiedOntologyUpdaterCli();
        ReflectionTestUtils.setField( cli, "sourcesDir", sourcesDir );
        // differs from -destDir below, so the command does not try to reindex the live service
        ReflectionTestUtils.setField( cli, "destDir", dir.resolve( "default-tdb" ) );

        assertThat( cli ).withArguments( "-skipDownload", "-destDir", destDir.toString() )
                .fails()
                .exitCause()
                .hasMessageContaining( "source file(s) are missing" );

        assertThat( destDir ).doesNotExist();
        assertThat( dir.resolve( "tdb.new" ) ).doesNotExist();
    }
}
