package ubic.gemma.core.analysis.sequence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static ubic.gemma.core.util.test.TestProcessUtils.FILL_STDERR;
import static ubic.gemma.core.util.test.TestProcessUtils.writeShellScript;

/**
 * gfClient and gfServer replaced by shell scripts that write more to standard error than a pipe buffer holds.
 */
@DisabledOnOs(OS.WINDOWS)
class ShellDelegatingBlatStderrTest {

    @TempDir
    Path tmp;

    private Object originalGfClientExe;
    private Object originalGfServerExe;
    private Object originalDownloadPath;
    private ShellDelegatingBlat blat;

    @BeforeEach
    void saveSettings() {
        originalGfClientExe = Settings.getProperty( "gfClient.exe" );
        originalGfServerExe = Settings.getProperty( "gfServer.exe" );
        originalDownloadPath = Settings.getProperty( "gemma.download.path" );
        // where the query and output files go
        Settings.setProperty( "gemma.download.path", tmp.toString() );
    }

    @AfterEach
    void restoreSettings() {
        if ( blat != null && blat.getServerProcess() != null && blat.getServerProcess().isAlive() ) {
            blat.getServerProcess().destroyForcibly();
        }
        Settings.setProperty( "gfClient.exe", originalGfClientExe );
        Settings.setProperty( "gfServer.exe", originalGfServerExe );
        Settings.setProperty( "gemma.download.path", originalDownloadPath );
    }

    /**
     * gfClient's stderr was read only after it exited, so one that wrote more than a pipe buffer's worth blocked, and
     * the query waited forever.
     */
    @Test
    void aFailingGfClientThatWritesALotToStandardErrorIsReported() throws Exception {
        Path gfClient = writeShellScript( tmp, "gfClient", FILL_STDERR + "\n"
                + "echo 'Sorry, the BLAT server seems to be down' >&2\n"
                + "exit 255" );
        Settings.setProperty( "gfClient.exe", gfClient.toString() );
        blat = new ShellDelegatingBlat();
        Taxon human = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );
        BioSequence bs = BioSequence.Factory.newInstance( "bs1", human );
        bs.setSequence( "GTCCTCGGAACCAGGACCTCGGCGTGGCCTAGCG" );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> blat.blatQuery( bs ) )
                        .hasMessageContaining( "gfClient exited with 255" )
                        .hasMessageEndingWith( "Sorry, the BLAT server seems to be down" ) );
    }

    /**
     * Nothing read gfServer's stderr while it ran, so a server that wrote more than a pipe buffer's worth to it
     * blocked and stopped answering.
     */
    @Test
    void aGfServerThatWritesALotToStandardErrorKeepsRunning() throws Exception {
        Path ready = tmp.resolve( "ready" );
        Path gfServer = writeShellScript( tmp, "gfServer", FILL_STDERR + "\n"
                + "touch '" + ready + "'\n"
                + "exec sleep 60" );
        Settings.setProperty( "gfServer.exe", gfServer.toString() );
        blat = new ShellDelegatingBlat();
        assumeFalse( blat.isServerReachable( ShellDelegatingBlat.BlattableGenome.HUMAN, false ),
                "Something is already listening on the human gfServer port." );

        blat.startServer( ShellDelegatingBlat.BlattableGenome.HUMAN, false, false );

        assertTimeoutPreemptively( Duration.ofSeconds( 30 ), () -> {
            while ( !Files.exists( ready ) ) {
                Thread.sleep( 50 );
            }
        }, "gfServer got past writing to its standard error" );
        assertThat( blat.getServerProcess().isAlive() ).isTrue();
        blat.stopServer();
    }
}
