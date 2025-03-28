package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

class LoggingBatchTaskSummaryWriter implements BatchTaskSummaryWriter {

    private final Log log;

    LoggingBatchTaskSummaryWriter( String logCategory ) {
        log = LogFactory.getLog( logCategory );
    }

    @Override
    public void write( BatchTaskProcessingResult result ) throws IOException {
        String message = result.getMessage();
        if ( result.getSource() != null ) {
            message = result.getSource() + ": " + message;
        }
        switch ( result.getResultType() ) {
            case SUCCESS:
                log.info( message, result.getThrowable() );
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
