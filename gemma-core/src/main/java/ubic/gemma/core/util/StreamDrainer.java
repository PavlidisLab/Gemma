package ubic.gemma.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Reads a stream to its end on a daemon thread, keeping the last {@code maxChars} characters.
 * <p>
 * Use it for a child process's standard error when the parent waits for the process, or reads its standard output,
 * before reading standard error. A child blocks once it has written a pipe buffer's worth (64 KiB on Linux) that
 * nobody reads, so without this such a child and the parent wait on each other forever.
 */
public class StreamDrainer {

    /**
     * Enough for an error message and its context; the start of a long output is dropped.
     */
    public static final int DEFAULT_MAX_CHARS = 64 * 1024;

    private static final String TRUNCATED = "[earlier output omitted]\n";

    /**
     * Start reading the stream on a new daemon thread, keeping the last {@link #DEFAULT_MAX_CHARS} characters.
     *
     * @param name for the thread, e.g. {@code "RepeatMasker stderr"}
     */
    public static StreamDrainer start( InputStream stream, String name ) {
        return start( stream, name, DEFAULT_MAX_CHARS );
    }

    public static StreamDrainer start( InputStream stream, String name, int maxChars ) {
        StreamDrainer drainer = new StreamDrainer( maxChars );
        drainer.thread = new Thread( () -> drainer.read( stream ), name );
        drainer.thread.setDaemon( true );
        drainer.thread.start();
        return drainer;
    }

    private final int maxChars;
    private final StringBuilder buffer = new StringBuilder();
    private boolean truncated = false;
    private Thread thread;

    private StreamDrainer( int maxChars ) {
        this.maxChars = maxChars;
    }

    private void read( InputStream stream ) {
        char[] chunk = new char[8192];
        try ( Reader reader = new InputStreamReader( stream, StandardCharsets.UTF_8 ) ) {
            int n;
            while ( ( n = reader.read( chunk ) ) != -1 ) {
                synchronized ( buffer ) {
                    buffer.append( chunk, 0, n );
                    if ( buffer.length() > maxChars ) {
                        buffer.delete( 0, buffer.length() - maxChars );
                        truncated = true;
                    }
                }
            }
        } catch ( IOException e ) {
            // the stream was closed under us, e.g. the process was destroyed; keep what was read
        }
    }

    /**
     * Wait up to the given time for the end of the stream, then return what was kept.
     * <p>
     * The end of a process's stream normally follows its exit closely, but a descendant that inherited the stream can
     * hold it open, hence the timeout.
     */
    public String await( long timeout, TimeUnit unit ) throws InterruptedException {
        thread.join( Math.max( 1, unit.toMillis( timeout ) ) );
        return getText();
    }

    /**
     * @return what has been kept so far
     */
    public String getText() {
        synchronized ( buffer ) {
            return truncated ? TRUNCATED + buffer : buffer.toString();
        }
    }
}
