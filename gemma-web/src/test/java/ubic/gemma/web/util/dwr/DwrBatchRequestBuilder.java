package ubic.gemma.web.util.dwr;

public class DwrBatchRequestBuilder {

    private final String servletPath;
    private final int batchId;

    public DwrBatchRequestBuilder( String servletPath, int batchId ) {
        this.servletPath = servletPath;
        this.batchId = batchId;
    }

    /**
     * Perform a DWR call.
     */
    public DwrRequestBuilder dwr( Class<?> clazz, String methodName, Object... args ) {
        return new DwrRequestBuilder( servletPath, clazz, methodName, batchId ).and( args );
    }
}
