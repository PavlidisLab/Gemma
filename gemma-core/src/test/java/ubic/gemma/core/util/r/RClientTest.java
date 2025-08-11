package ubic.gemma.core.util.r;

import org.junit.Test;
import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.REXPMismatchException;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RClientTest {

    @Test
    public void testStandaloneREngine() throws REXPMismatchException {
        try ( RClient client = new RClient( () -> new StandaloneRConnection( Paths.get( "Rscript" ) ) ) ) {
            assertEquals( "Hello!", client.parseAndEval( "'Hello!'" ).asString() );
        }
    }

    @Test
    public void testJRIEngine() throws REXPMismatchException {
        try ( RClient client = new RClient( JRIEngine::new ) ) {
            assertEquals( "Hello!", client.parseAndEval( "'Hello!'" ).asString() );
        }
    }

    @Test
    public void testAssignDataFrame() {
        try ( RClient client = new RClient( JRIEngine::new ) ) {
            List<String> colNames = Arrays.asList( "a", "b", "c" );
            List<String> rowNames = Arrays.asList( "row1", "row2", "row3" );
            List<Object> data = Arrays.asList(
                    new double[] { 1, 2, 3 },
                    new double[] { 4, 5, 6 },
                    new double[] { 7, 8, 9 }
            );
            client.assignDataFrame( "foo", colNames, rowNames, data );
        }
    }
}