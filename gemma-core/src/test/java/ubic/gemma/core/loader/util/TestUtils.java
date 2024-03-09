package ubic.gemma.core.loader.util;

import org.apache.commons.logging.Log;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class TestUtils {

    /**
     * Checks whether the given Exception is a known NCBI error and if so, logs the error.
     *
     * @param log the log to use to log the error.
     * @param e   the exception to check for known errors.
     * @return true, if the exception was handled by this method and logged, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    public static boolean LogNcbiError( Log log, Exception e ) {
        if ( e.getCause() instanceof ExecutionException ) {
            log.error( "Failed to get file -- skipping rest of test" );
            return true;
        } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
            log.error( "Failed to connect to NCBI, skipping test" );
            return true;
        } else if ( e.getCause() instanceof org.apache.commons.net.ftp.FTPConnectionClosedException ) {
            log.error( "Failed to connect to NCBI, skipping test" );
            return true;
        }
        return false;
    }

    /**
     * Checks the BioAssays in the given EE match the given properties. Sequence read is always asserted to be 36.
     *
     * @param targetArrayDesign the AD that the BAs should be using.
     * @param ee                the experiment to get the BAs from.
     * @param accession         the accession the BAs should have in their description.
     * @param readCount         the sequence read count.
     */
    public static void assertBAs( ExpressionExperiment ee, ArrayDesign targetArrayDesign, String accession,
            int readCount ) {
        boolean found = false;
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );

            assertNotNull( ba.getSequenceReadLength() );
            assertEquals( 36, ba.getSequenceReadLength().intValue() );

            if ( ba.getDescription().contains( accession ) ) {
                assertNotNull( ba.getSequenceReadCount() );
                assertEquals( readCount, ba.getSequenceReadCount().intValue() );
                found = true;
            }
        }
        assertTrue( found );
    }

}
