package ubic.gemma.web.util.dwr;

import org.directwebremoting.impl.DefaultContainer;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletConfig;

public class MockDwrRequestBuilders {

    /**
     * TODO: infer this from the servlet configuration
     */
    private static final String SERVLET_PATH = "/dwr";

    /**
     * Request the DWR static page.
     * @see org.directwebremoting.impl.ContainerUtil#setupDefaults(DefaultContainer, ServletConfig)
     */
    public static RequestBuilder dwrStaticPage( String page ) {
        return MockMvcRequestBuilders.get( SERVLET_PATH + page )
                .servletPath( SERVLET_PATH );
    }

    /**
     * Perform a DWR call.
     */
    public static DwrRequestBuilder dwr( Class<?> clazz, String methodName, Object... args ) {
        return new DwrRequestBuilder( SERVLET_PATH, clazz, methodName, 0 ).and( args );
    }

    /**
     * Perform a batched DWR call.
     */
    public static DwrBatchRequestBuilder dwrBatch( int batchId ) {
        return new DwrBatchRequestBuilder( SERVLET_PATH, batchId );
    }
}
