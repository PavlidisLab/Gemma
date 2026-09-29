package ubic.gemma.core.loader.genome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static ubic.gemma.core.util.test.TestProcessUtils.FILL_STDERR;
import static ubic.gemma.core.util.test.TestProcessUtils.writeShellScript;

/**
 * blastdbcmd replaced by a shell script that writes more to standard error than a pipe buffer holds before it writes
 * anything to standard output. {@link SimpleFastaCmd} read standard output to its end first, so the two waited on each
 * other forever.
 */
@DisabledOnOs(OS.WINDOWS)
class SimpleFastaCmdStderrTest {

    @TempDir
    Path tmp;

    @Test
    void entriesNotFoundAreAWarning() throws Exception {
        Path exe = writeShellScript( tmp, "blastdbcmd", FILL_STDERR + "\n"
                + "echo 'Error: [blastdbcmd] Skipped AB000002' >&2\n"
                + "printf '>AB000001\\nACGTACGTACGT\\n'\n"
                + "exit 1" );
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd( exe.toString() );
        fastaCmd.setBlastHome( tmp );

        Collection<BioSequence> sequences = assertTimeoutPreemptively( Duration.ofSeconds( 60 ),
                () -> fastaCmd.getBatchAccessions( Collections.singletonList( "AB000001" ), "testdb" ) );

        assertThat( sequences ).hasSize( 1 );
    }

    @Test
    void aFailureReportsTheEndOfStandardError() throws Exception {
        Path exe = writeShellScript( tmp, "blastdbcmd", FILL_STDERR + "\n"
                + "echo 'BLAST Database error: No alias or index file found' >&2\n"
                + "exit 2" );
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd( exe.toString() );
        fastaCmd.setBlastHome( tmp );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> fastaCmd.getByAccession( "AB000001", "testdb" ) )
                        .hasMessageContaining( "exit value=2" )
                        .hasMessageEndingWith( "No alias or index file found" ) );
    }
}
