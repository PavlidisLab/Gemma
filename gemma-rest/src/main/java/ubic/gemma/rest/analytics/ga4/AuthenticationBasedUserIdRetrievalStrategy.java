package ubic.gemma.rest.analytics.ga4;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Strategy for the user ID that uses the authentication object from the {@link SecurityContextHolder}.
 * @author poirigui
 */
@CommonsLog
public class AuthenticationBasedUserIdRetrievalStrategy implements UserIdRetrievalStrategy {

    private final AuthenticationTrustResolver authenticationTrustResolver;

    public AuthenticationBasedUserIdRetrievalStrategy( AuthenticationTrustResolver authenticationTrustResolver ) {
        this.authenticationTrustResolver = authenticationTrustResolver;
    }

    @Override
    public String get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth != null ) {
            // check if anonymous
            if ( authenticationTrustResolver.isAnonymous( auth ) ) {
                return null;
            } else if ( auth.getPrincipal() instanceof String ) {
                return ( String ) auth.getPrincipal();
            } else if ( auth.getPrincipal() instanceof UserDetails ) {
                return ( ( UserDetails ) auth.getPrincipal() ).getUsername();
            } else {
                log.warn( String.format( "Don't know how to extract a user ID from %s.", auth.getPrincipal() ) );
                return null;
            }
        } else {
            return null;
        }
    }
}
