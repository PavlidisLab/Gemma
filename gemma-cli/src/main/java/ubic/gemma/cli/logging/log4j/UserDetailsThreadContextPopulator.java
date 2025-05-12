package ubic.gemma.cli.logging.log4j;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ubic.gemma.cli.authentication.CLIAuthenticationAware;
import ubic.gemma.core.logging.log4j.ThreadContextPopulator;

/**
 * Populate user details in the Log4j {@link ThreadContext}.
 * @author poirigui
 */
@CommonsLog
@Component
public class UserDetailsThreadContextPopulator implements ThreadContextPopulator, CLIAuthenticationAware {

    public static final String CURRENT_USER_CONTEXT_KEY = KEY_PREFIX + ".currentUser";

    @Override
    public String getKey() {
        return CURRENT_USER_CONTEXT_KEY;
    }

    @Override
    public void populate() {
        // do nothing now
    }

    @Override
    public void setAuthentication( Authentication auth ) {
        Object principal = auth.getPrincipal();
        if ( principal instanceof UserDetails ) {
            ThreadContext.put( CURRENT_USER_CONTEXT_KEY, ( ( UserDetails ) principal ).getUsername() );
        } else if ( principal instanceof String ) {
            ThreadContext.put( CURRENT_USER_CONTEXT_KEY, ( String ) principal );
        } else {
            log.warn( "Unsupported principal type " + principal.getClass().getName() );
            ThreadContext.remove( CURRENT_USER_CONTEXT_KEY );
        }
    }

    @Override
    public void clearAuthentication() {
        ThreadContext.remove( CURRENT_USER_CONTEXT_KEY );
    }
}
