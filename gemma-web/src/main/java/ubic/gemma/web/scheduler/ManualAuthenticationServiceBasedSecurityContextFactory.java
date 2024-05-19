package ubic.gemma.web.scheduler;

import gemma.gsec.authentication.ManualAuthenticationService;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Creates a security context based using manual authentication.
 * @author poirigui
 */
@Setter
@CommonsLog
public class ManualAuthenticationServiceBasedSecurityContextFactory extends AbstractFactoryBean<SecurityContext> {

    private final ManualAuthenticationService manualAuthenticationService;

    private String userName;
    private String password;

    /**
     * Fallback to an anonymous authentication if the authentication fails.
     */
    private boolean fallbackToAnonymous = false;

    public ManualAuthenticationServiceBasedSecurityContextFactory( ManualAuthenticationService manualAuthenticationService ) {
        this.manualAuthenticationService = manualAuthenticationService;
    }

    @Override
    public Class<?> getObjectType() {
        return SecurityContext.class;
    }

    @Override
    protected SecurityContext createInstance() {
        try {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication( manualAuthenticationService.attemptAuthentication( userName, password ) );
            return securityContext;
        } catch ( AuthenticationException e ) {
            if ( fallbackToAnonymous ) {
                log.error( "Failed to authenticate schedule job, jobs probably won't work, but trying anonymous.", e );
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                SecurityContextHolder.setContext( securityContext );
                // gsec will call SecurityContextHolder.getContext().setAuthentication()
                SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
                try {
                    manualAuthenticationService.authenticateAnonymously();
                    return securityContext;
                } finally {
                    SecurityContextHolder.clearContext();
                    SecurityContextHolder.setContext( previousSecurityContext );
                }
            } else {
                throw e;
            }
        }
    }
}
