package ubic.gemma.web.util.dwr;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Request builder that allows multiple DWR calls to be performed.
 * @author poirigui
 */
public class DwrRequestBuilder implements RequestBuilder {

    private final String servletPath;
    private final Class<?> clazz;
    private final String methodName;
    private final int batchId;

    // internal state
    private final StringBuilder callPayload;
    private int callCount = 0;

    public DwrRequestBuilder( String servletPath, Class<?> clazz, String methodName, int batchId ) {
        this.servletPath = servletPath;
        this.clazz = clazz;
        this.methodName = methodName;
        this.batchId = batchId;
        this.callPayload = new StringBuilder();
    }

    /**
     * Chain one additional DWR call.
     */
    public DwrRequestBuilder and( Object... args ) {
        int callId = callCount;
        callPayload
                .append( "c" ).append( callId ).append( "-scriptName=" ).append( clazz.getSimpleName() ).append( '\n' )
                .append( "c" ).append( callId ).append( "-methodName=" ).append( methodName ).append( '\n' )
                .append( "c" ).append( callId ).append( "-id=" ).append( callId ).append( '\n' );
        int i = 0;
        for ( Object arg : args ) {
            String type;
            if ( arg instanceof String ) {
                type = "string";
            } else if ( arg instanceof Number ) {
                type = "number";
            } else if ( arg instanceof Boolean ) {
                type = "boolean";
            } else {
                throw new IllegalArgumentException( "Unsupported type: " + arg.getClass() );
            }
            try {
                callPayload.append( "c" ).append( callId ).append( "-param" ).append( i++ ).append( "=" ).append( type ).append( ":" )
                        .append( URLEncoder.encode( String.valueOf( arg ), StandardCharsets.UTF_8.name() ) )
                        .append( '\n' );
            } catch ( UnsupportedEncodingException e ) {
                throw new RuntimeException( e );
            }
        }
        callCount++;
        return this;
    }

    @Override
    public MockHttpServletRequest buildRequest( ServletContext servletContext ) {
        String payload = "callCount=" + callCount + "\n" +
                "page=\n" +
                "httpSessionId=\n" +
                "scriptSessionId=\n" +
                callPayload +
                "batchId=" + batchId + "\n";
        return MockMvcRequestBuilders
                .post( servletPath + "/call/plaincall/{className}.{methodName}.dwr", clazz.getSimpleName(), methodName )
                .servletPath( servletPath )
                .content( payload )
                .buildRequest( servletContext );
    }
}
