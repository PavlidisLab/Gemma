package ubic.gemma.web.util.dwr;

import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class MockDwrRequestBuilders {

    /**
     * TODO: infer this from the servlet configuration
     */
    private static final String SERVLET_PATH = "/dwr";

    /**
     * Request the DWR index.
     */
    public static RequestBuilder dwrIndex() {
        return MockMvcRequestBuilders.get( SERVLET_PATH + "/index.html" )
                .servletPath( SERVLET_PATH );
    }

    /**
     * Perform a DWR call.
     */
    public static DwrRequestBuilder dwr( Class<?> clazz, String methodName, Object... args ) {
        return new DwrRequestBuilder( SERVLET_PATH, clazz, methodName ).and( args );
    }
}
