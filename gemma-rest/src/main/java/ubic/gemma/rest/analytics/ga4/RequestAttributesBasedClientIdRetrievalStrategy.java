package ubic.gemma.rest.analytics.ga4;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import ubic.gemma.core.lang.Nullable;

import static ubic.gemma.rest.analytics.ga4.GoogleAnalytics4Provider.isValidClientId;

/**
 * Strategy that retrieves and store client IDs in the {@link RequestAttributes}.
 * @see RequestContextHolder#currentRequestAttributes()
 * @author poirigui
 */
@CommonsLog
public class RequestAttributesBasedClientIdRetrievalStrategy implements ClientIdRetrievalStrategy {

    private static final String DEFAULT_ATTRIBUTE = "clientId";
    private static final int DEFAULT_SCOPE = RequestAttributes.SCOPE_REQUEST;

    private String attribute = DEFAULT_ATTRIBUTE;
    private int scope = DEFAULT_SCOPE;

    @Nullable
    @Override
    public String get() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        Object clientId = requestAttributes.getAttribute( "clientId", scope );
        if ( clientId instanceof String && isValidClientId( ( String ) clientId ) ) {
            return ( String ) clientId;
        } else if ( clientId == null ) {
            clientId = RandomStringUtils.randomNumeric( 10 ) + "." + RandomStringUtils.randomNumeric( 10 );
            try {
                requestAttributes.setAttribute( "clientId", clientId, scope );
            } catch ( IllegalStateException e ) {
                log.trace( "Request attributes are not writable, will not be using the generated client ID." );
                return null;
            }
            return ( String ) clientId;
        } else {
            log.trace( String.format( "Non-string or invalid client ID stored for key %s in request attributes.", attribute ) );
            return null;
        }
    }

    /**
     * Set the attribute to retrieve to obtain the client ID.
     */
    public void setAttribute( String attribute ) {
        this.attribute = attribute;
    }

    /**
     * Set the scope to use for retrieving and storing the client ID.
     *
     * @see RequestAttributes#SCOPE_REQUEST
     * @see RequestAttributes#SCOPE_SESSION
     */
    public void setScope( int scope ) {
        this.scope = scope;
    }
}
