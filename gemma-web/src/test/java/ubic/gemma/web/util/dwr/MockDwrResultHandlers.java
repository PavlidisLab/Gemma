package ubic.gemma.web.util.dwr;

import java.util.function.Consumer;

/**
 * Handles DWR results.
 * @author poirigui
 */
public class MockDwrResultHandlers {

    public static DwrBatchHandlers getBatch( int batchId ) {
        return new DwrBatchHandlers( batchId );
    }

    public static <T> DwrCallbackHandler<T> getCallback( int callId, Consumer<T> doWithReply ) {
        return new DwrCallbackHandler<>( 0, callId, doWithReply );
    }

    public static <T> DwrCallbackHandler<T> getCallback( Consumer<T> doWithReply ) {
        return getCallback( 0, doWithReply );
    }

    public static DwrExceptionHandler getException( int callId, Consumer<DwrException> doWithException ) {
        return new DwrExceptionHandler( 0, callId, doWithException );
    }

    public static DwrExceptionHandler getException( Consumer<DwrException> doWithException ) {
        return getException( 0, doWithException );
    }
}