package ubic.gemma.cli.batch;

import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;
import java.util.List;

@CommonsLog
public class CompositeBatchTaskSummaryWriter implements BatchTaskSummaryWriter {

    private final List<BatchTaskSummaryWriter> writers;

    public CompositeBatchTaskSummaryWriter( List<BatchTaskSummaryWriter> writers ) {
        this.writers = writers;
    }

    @Override
    public void write( BatchTaskProcessingResult result ) throws IOException {
        Exception firstException = null;
        for ( BatchTaskSummaryWriter writer : writers ) {
            try {
                writer.write( result );
            } catch ( Exception e ) {
                if ( firstException == null ) {
                    firstException = e;
                } else {
                    log.error( "Failed to write to " + writer + ", but an exception was already raised by another writer.", e );
                }
            }
        }
        if ( firstException != null ) {
            throwAsIOException( firstException );
        }
    }

    @Override
    public void close() throws IOException {
        Exception firstException = null;
        for ( BatchTaskSummaryWriter writer : writers ) {
            try {
                writer.close();
            } catch ( Exception e ) {
                if ( firstException == null ) {
                    firstException = e;
                } else {
                    log.error( "Failed to close " + writer + ", but an exception was already raised by another writer.", e );
                }
            }
        }
        if ( firstException != null ) {
            throwAsIOException( firstException );
        }
    }

    /**
     * Re-raise an exception caught from a delegate writer. {@link BatchTaskSummaryWriter#write} and
     * {@link BatchTaskSummaryWriter#close} are only declared to throw {@link IOException}, so any
     * non-IOException checked exception (which delegates should not be raising) is wrapped.
     * Runtime exceptions pass through unchanged.
     */
    private static void throwAsIOException( Exception e ) throws IOException {
        if ( e instanceof IOException ) {
            throw ( IOException ) e;
        } else if ( e instanceof RuntimeException ) {
            throw ( RuntimeException ) e;
        } else {
            throw new IOException( e );
        }
    }
}
