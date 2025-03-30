package ubic.gemma.cli.util;

import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;
import java.util.List;

@CommonsLog
class CompositeBatchTaskSummaryWriter implements BatchTaskSummaryWriter {

    private final List<BatchTaskSummaryWriter> writers;

    CompositeBatchTaskSummaryWriter( List<BatchTaskSummaryWriter> writers ) {
        this.writers = writers;
    }

    @Override
    @SneakyThrows
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
            throw firstException;
        }
    }

    @Override
    @SneakyThrows
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
            throw firstException;
        }
    }
}
