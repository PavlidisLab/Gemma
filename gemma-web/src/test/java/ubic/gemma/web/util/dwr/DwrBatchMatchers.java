package ubic.gemma.web.util.dwr;

/**
 * Match DWR batch results.
 * @author poirigui
 */
public class DwrBatchMatchers {

    private final int batchId;

    public DwrBatchMatchers( int batchId ) {
        this.batchId = batchId;
    }

    /**
     * Match a DWR callback.
     */
    public DwrCallbackMatchers callback( int callId ) {
        return new DwrCallbackMatchers( batchId, callId );
    }

    public DwrCallbackMatchers callback() {
        return callback( 0 );
    }

    /**
     * Match a DWR exception.
     */
    public DwrExceptionMatchers exception( int callId ) {
        return new DwrExceptionMatchers( batchId, callId );
    }

    public DwrExceptionMatchers exception() {
        return exception( 0 );
    }
}
