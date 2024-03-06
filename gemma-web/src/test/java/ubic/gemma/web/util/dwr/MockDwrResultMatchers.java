package ubic.gemma.web.util.dwr;

/**
 * Match DWR results.
 * @author poirigui
 */
public class MockDwrResultMatchers {

    /**
     * Match a DWR callback.
     */
    public static DwrCallbackMatchers callback( int callId ) {
        return new DwrCallbackMatchers( 0, callId );
    }

    public static DwrCallbackMatchers callback() {
        return callback( 0 );
    }

    /**
     * Match a DWR exception.
     */
    public static DwrExceptionMatchers exception( int callId ) {
        return new DwrExceptionMatchers( 0, callId );
    }

    public static DwrExceptionMatchers exception() {
        return exception( 0 );
    }
}
