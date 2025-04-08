package ubic.gemma.cli.batch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Task summary writer that logs the result of a batch task to a logger.
 * @author poirigui
 */
public class LoggingBatchTaskSummaryWriter implements BatchTaskSummaryWriter {

    private final Log log;
    private final boolean useDebugForSuccess;

    public LoggingBatchTaskSummaryWriter( String logCategory ) {
        this( logCategory, false );
    }

    /**
     * @param logCategory        log category to use to report batch processing results
     * @param useDebugForSuccess if true, success are reported as debug logs
     */
    public LoggingBatchTaskSummaryWriter( String logCategory, boolean useDebugForSuccess ) {
        this.log = LogFactory.getLog( logCategory );
        this.useDebugForSuccess = useDebugForSuccess;
    }

    @Override
    public void write( BatchTaskProcessingResult result ) throws IOException {
        String message;
        if ( result.getSource() != null ) {
            if ( result.getMessage() != null ) {
                message = result.getSource() + ": " + result.getMessage();
            } else {
                message = String.valueOf( result.getSource() );
            }
        } else if ( result.getMessage() != null ) {
            message = result.getMessage();
        } else {
            message = "No object nor message provided.";
        }
        switch ( result.getResultType() ) {
            case SUCCESS:
                if ( useDebugForSuccess ) {
                    log.debug( message, result.getThrowable() );
                } else {
                    log.info( message, result.getThrowable() );
                }
                break;
            case WARNING:
                log.warn( message, result.getThrowable() );
                break;
            case ERROR:
                log.error( message, result.getThrowable() );
                break;
            default:
                throw new IllegalArgumentException( "Unknown result type: " + result.getResultType() );
        }
    }

    @Override
    public void close() throws IOException {

    }
}
