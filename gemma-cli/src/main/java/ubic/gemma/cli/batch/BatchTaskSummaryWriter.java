package ubic.gemma.cli.batch;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author poirigui
 */
public interface BatchTaskSummaryWriter extends Closeable {

    /**
     * Write the result of a batch task.
     * <p>
     * The actual writing may be deferred until the writer is closed in {@link #close()}.
     */
    void write( BatchTaskProcessingResult result ) throws IOException;

    /**
     * Close the writer and write any remaining results.
     */
    @Override
    void close() throws IOException;
}
