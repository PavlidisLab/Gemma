package ubic.gemma.core.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@ParametersAreNonnullByDefault
public class ProgressInputStream extends FilterInputStream {

    private final ProgressReporter progressReporter;
    private long progressInBytes = 0;
    private final long maxSizeInBytes;
    private boolean reportedAtEof = false;

    /**
     * @param maxSizeInBytes the maximum size in bytes
     */
    public ProgressInputStream( InputStream in, ProgressReporter progressReporter, long maxSizeInBytes ) {
        super( in );
        this.progressReporter = progressReporter;
        this.maxSizeInBytes = maxSizeInBytes;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        return recordProgress( super.read( b, off, len ), false );
    }

    @Override
    public int read() throws IOException {
        return recordProgress( super.read(), true );
    }

    @Override
    public long skip( long n ) throws IOException {
        return recordProgress( super.skip( n ), false );
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            progressReporter.close();
        }
    }

    private int recordProgress( int read, boolean oneByte ) {
        return ( int ) recordProgress( ( long ) read, oneByte );
    }

    private long recordProgress( long read, boolean oneByte ) {
        if ( read < 0 ) {
            if ( !reportedAtEof ) {
                progressReporter.reportProgress( progressInBytes, maxSizeInBytes, true );
                reportedAtEof = true;
            }
        } else {
            progressInBytes += oneByte ? 1 : read;
            progressReporter.reportProgress( progressInBytes, maxSizeInBytes, false );
        }
        return read;
    }
}
