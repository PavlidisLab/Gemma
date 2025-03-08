package ubic.gemma.core.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Test script to validate locking behaviors for {@link ReadWriteFileLockTest}.
 * @author poirigui
 */
public class ReadWriteFileLockTestScript {

    public static void main( String[] args ) {
        boolean shared = args[1].equals( "shared" );
        try ( FileChannel channel = FileChannel.open( Paths.get( args[0] ), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE ) ) {
            System.out.println( "Acquiring " + args[1] + " lock on " + args[0] + "..." );
            try ( FileLock lock = channel.tryLock( 0, Long.MAX_VALUE, shared ) ) {
                if ( lock != null ) {
                    System.out.println( "Lock acquired." );
                    System.exit( 0 );
                } else {
                    System.out.println( "Lock not acquired." );
                    System.exit( 2 );
                }
            }
        } catch ( IOException e ) {
            e.printStackTrace( System.err );
            System.exit( 1 );
        }
    }
}
