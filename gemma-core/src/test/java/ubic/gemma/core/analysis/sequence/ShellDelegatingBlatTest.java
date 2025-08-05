package ubic.gemma.core.analysis.sequence;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExecutableExists;

public class ShellDelegatingBlatTest {

    private ShellDelegatingBlat sdb;

    @After
    public void shutdownBlatServer() {
        if ( sdb != null ) {
            sdb.stopServer();
        }
    }

    @Test
    public void testClient() throws IOException {
        sdb = new ShellDelegatingBlat();
        assumeThatExecutableExists( sdb.getGfClientExe() );
        assumeTrue( "The gfServer for human is not reachable.", sdb.isServerReachable( ShellDelegatingBlat.BlattableGenome.HUMAN, false ) );
        Taxon taxon = Taxon.Factory.newInstance( "human" );
        BioSequence bs = BioSequence.Factory.newInstance( "bs1", taxon );
        bs.setSequence( "GTCCTCGGAACCAGGACCTCGGCGTGGCCTAGCG" );
        sdb.blatQuery( bs );
    }

    @Test
    @Category(SlowTest.class)
    @Ignore("This works, but it is way too slow.")
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