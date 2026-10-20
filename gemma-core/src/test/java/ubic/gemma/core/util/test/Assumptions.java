package ubic.gemma.core.util.test;

import org.apache.commons.io.FileUtils;
import ubic.gemma.core.util.runtime.ExtendedRuntime;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

/**
 * Reusable assumptions for tests.
 *
 * @author poirigui
 */
public class Assumptions {

    /**
     * Check if an executable exists and is executable.
     * <p>
     * If the executable is a path, it will be checked directly, otherwise it will be searched in the $PATH.
     */
    public static void assumeThatExecutableExists( String executable ) {
        if ( executable.contains( "/" ) ) {
            assumeTrue( Files.isExecutable( Paths.get( executable ) ) );
        } else {
            boolean found = false;
            String pathEnv = System.getenv( "PATH" );
            if ( pathEnv != null ) {
                for ( String p : pathEnv.split( ":" ) ) {
                    if ( Files.isExecutable( Paths.get( p, executable ) ) ) {
                        found = true;
                        break;
                    }
                }
            }
            assumeTrue( "Executable " + executable + " not found in $PATH (" + pathEnv + ").", found );
        }
    }

    /**
     * Assume that a certain amount of memory is available.
     *
     * @param jvm whether to consider the free JVM memory or the system free memory
     */
    public static void assumeThatFreeMemoryIsGreaterOrEqualTo( long bytes, boolean jvm ) {
        long f = ( jvm ? Runtime.getRuntime().freeMemory() : getSystemFreeMemory() );
        assumeTrue( String.format( "At least %s of free memory is required to run this test, only %s was available.",
                FileUtils.byteCountToDisplaySize( bytes ), FileUtils.byteCountToDisplaySize( f ) ), f >= bytes );
    }

    private static long getSystemFreeMemory() {
        try {
            return ExtendedRuntime.getRuntime().getMemInfo().getAvailableMemory();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
