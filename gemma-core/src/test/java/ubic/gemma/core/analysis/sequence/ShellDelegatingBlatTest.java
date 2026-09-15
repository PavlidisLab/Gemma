package ubic.gemma.core.analysis.sequence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExecutableExists;

public class ShellDelegatingBlatTest {

    private ShellDelegatingBlat sdb;

    @AfterEach
    public void shutdownBlatServer() {
        if ( sdb != null ) {
            sdb.stopServer();
        }
    }

    @Test
    public void testClient() throws IOException {
        sdb = new ShellDelegatingBlat();
        assumeThatExecutableExists( sdb.getGfClientExe() );
        assumeTrue( sdb.isServerReachable( ShellDelegatingBlat.BlattableGenome.HUMAN, false ), "The gfServer for human is not reachable." );
        Taxon taxon = Taxon.Factory.newInstance( "human" );
        BioSequence bs = BioSequence.Factory.newInstance( "bs1", taxon );
        bs.setSequence( "GTCCTCGGAACCAGGACCTCGGCGTGGCCTAGCG" );
        sdb.blatQuery( bs );
    }

    @Test
    @Tag("slow")
    @Disabled("This works, but it is way too slow.")
    public void testServer() throws IOException {
        sdb = new ShellDelegatingBlat();
        assumeThatExecutableExists( sdb.getGfClientExe() );
        assumeThatExecutableExists( sdb.getGfServerExe() );
        // this is very slow...
        sdb.startServer( ShellDelegatingBlat.BlattableGenome.HUMAN, false, true );
        assertTrue( sdb.isServerRunning() );
        Taxon taxon = Taxon.Factory.newInstance( "human" );
        BioSequence bs = BioSequence.Factory.newInstance( "bs1", taxon );
        bs.setSequence( "GTCCTCGGAACCAGGACCTCGGCGTGGCCTAGCG" );
        sdb.blatQuery( bs );
    }
}