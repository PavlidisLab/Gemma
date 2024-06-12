package ubic.gemma.web.util.dwr;

import java.util.function.Consumer;

/**
 * Handles DWR batches.
 * @author poirigui
 */
public class DwrBatchHandlers {

    private final int batchId;

    public DwrBatchHandlers( int batchId ) {
        this.batchId = batchId;
    }

    public <T> DwrCallbackHandler<T> getCallback( int callId, Consumer<T> doWithReply ) {
        return new DwrCallbackHandler<>( batchId, callId, doWithReply );
    }

    public <T> DwrCallbackHandler<T> getCallback( Consumer<T> doWithReply ) {
        return getCallback( batchId, doWithReply );
    }

    public DwrExceptionHandler getException( int callId, Consumer<DwrException> doWithException ) {
        return new DwrExceptionHandler( batchId, callId, doWithException );
    }

    public DwrExceptionHandler getException( Consumer<DwrException> doWithException ) {
        return getException( batchId, doWithException );
    }
}
