package ubic.gemma.core.util;

import org.apache.commons.io.file.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

/**
 * @author poirigui
 */
public class FileUtils {

    /**
     * Open a GZIP-compressed file safely.
     * <p>
     * This ensures that if an exception is thrown by {@link GZIPInputStream#GZIPInputStream(InputStream)}, the stream
     * will be closed.
     */
    public static InputStream openCompressedFile( Path path ) throws IOException {
        InputStream is = Files.newInputStream( path );
        try {
            return new GZIPInputStream( is );
        } catch ( Exception e ) {
            is.close();
            throw e;
        }
    }

    /**
     * Writes a file or a directory at a given path, handed to it by {@link #writeAtomically(Path, PathWriter)}.
     */
    @FunctionalInterface
    public interface PathWriter<E extends Exception> {

        /**
         * @param path a path that does not exist yet; the writer creates it, as a regular file or a directory
         */
        void write( Path path ) throws IOException, E;
    }

    /**
     * Write {@code destination} through a temporary sibling that is moved over it only once {@code writer} has
     * returned normally.
     * <p>
     * A partial result is never at {@code destination}: not when the writer throws, an {@link Error} included, and
     * not when the JVM dies or a daemon writer thread is halted mid-write, which no catch block can cover. On
     * failure, {@code destination} keeps whatever it held before.
     * <p>
     * The temporary is named {@code .<name>.tmp-<random>} and lives in the same directory, so the move is a rename
     * within one file system. It is deleted on failure. One left behind by a JVM that died stays until removed by
     * hand; its name ends in neither the destination's name nor its suffix, so a lookup by name or by suffix does not
     * find it, but code that lists the directory must skip it.
     * <p>
     * A directory already at {@code destination} is deleted just before the move, because a rename cannot replace a
     * non-empty directory: a crash between the two leaves no directory rather than a partial one.
     * <p>
     * The temporary is not synced to disk before the move, so this covers the process dying, not the host crashing.
     * Nothing here locks {@code destination}: a caller that coordinates with readers holds its lock on
     * {@code destination} across this call.
     *
     * @param destination the file or directory to create or replace
     * @param writer      writes the content at the temporary path it is given
     */
    public static <E extends Exception> void writeAtomically( Path destination, PathWriter<E> writer ) throws IOException, E {
        PathUtils.createParentDirectories( destination );
        Path tmp = destination.resolveSibling( "." + destination.getFileName() + ".tmp-"
                + Long.toUnsignedString( ThreadLocalRandom.current().nextLong(), 36 ) );
        try {
            writer.write( tmp );
            if ( Files.isDirectory( tmp, LinkOption.NOFOLLOW_LINKS ) && Files.isDirectory( destination, LinkOption.NOFOLLOW_LINKS ) ) {
                PathUtils.deleteDirectory( destination );
            }
            Files.move( tmp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
        } catch ( Throwable t ) {
            deleteQuietly( tmp, t );
            throw t;
        }
    }

    private static void deleteQuietly( Path tmp, Throwable cause ) {
        try {
            if ( Files.isDirectory( tmp, LinkOption.NOFOLLOW_LINKS ) ) {
                PathUtils.deleteDirectory( tmp );
            } else {
                Files.deleteIfExists( tmp );
            }
        } catch ( IOException | RuntimeException e ) {
            cause.addSuppressed( e );
        }
    }
}
