package ubic.gemma.web.scheduler;

import gemma.gsec.authentication.ManualAuthenticationService;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;

/**
 * Invoke a callable as an authenticated user with given credentials.
 * @author poirigui
 */
@Setter
@CommonsLog
public class SecureInvoker {

    private final ManualAuthenticationService manualAuthenticationService;

    private String userName;
    private String password;

    /**
     * Fallback to an anonymous authentication if the authentication fails.
     */
    private boolean fallbackToAnonymous = false;

    public SecureInvoker( ManualAuthenticationService manualAuthenticationService ) {
        this.manualAuthenticationService = manualAuthenticationService;
    }

    public <T> T invoke( Callable<T> callable ) throws Exception {
        SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
        try {
            try {
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication( manualAuthenticationService.attemptAuthentication( userName, password ) );
                SecurityContextHolder.setContext( securityContext );
            } catch ( AuthenticationException e ) {
                if ( fallbackToAnonymous ) {
                    log.error( "Failed to authenticate schedule job, jobs probably won't work, but trying anonymous" );
                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    SecurityContextHolder.setContext( securityContext );
                    // gsec will call SecurityContextHolder.getContext().setAuthentication()
                    manualAuthenticationService.authenticateAnonymously();
                } else {
                    throw e;
                }
            }
            assert SecurityContextHolder.getContext().getAuthentication() != null;
            return callable.call();
        } finally {
            SecurityContextHolder.setContext( previousSecurityContext );
        }
    }
}
