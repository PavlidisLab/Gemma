package ubic.gemma.rest.analytics.ga4;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Strategy that retrieves the client ID in a request header.
 * <p>
 * If this is used in conjunction with CORS, you will have to include the header in {@code Access-Control-Allow-Headers}.
 * @author poirigui
 */
public class RequestHeaderBasedClientIdRetrievalStrategy implements ClientIdRetrievalStrategy {

    public static final String DEFAULT_REQUEST_HEADER = "X-Gemma-Client-ID";

    private String requestHeader = DEFAULT_REQUEST_HEADER;

    @Override
    public String get() {
        HttpServletRequest request;
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        if ( requestAttributes instanceof ServletRequestAttributes ) {
            request = ( ( ServletRequestAttributes ) requestAttributes ).getRequest();
        } else {
            throw new UnsupportedOperationException( String.format( "Unsupported ServletRequestAttributes: %s.", RequestContextHolder.currentRequestAttributes() ) );
        }
        String clientId = request.getHeader( requestHeader );
        if ( clientId != null && GoogleAnalytics4Provider.isValidClientId( clientId ) ) {
            return clientId;
        } else {
            return null;
        }
    }

    /**
     * Set the request header used for retrieving the client ID.
     */
    public void setRequestHeader( String requestHeader ) {
        this.requestHeader = requestHeader;
    }
}
