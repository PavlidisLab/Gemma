package ubic.gemma.web.scheduler;

import gemma.gsec.authentication.ManualAuthenticationService;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

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

    public ManualAuthenticationServiceBasedSecurityContextFactory( ManualAuthenticationService manualAuthenticationService ) {
        this.manualAuthenticationService = manualAuthenticationService;
    }

    @Override
    public Class<?> getObjectType() {
        return SecurityContext.class;
    }

    @Override
    protected SecurityContext createInstance() {
        Assert.notNull( userName, "A username must be supplied." );
        Assert.notNull( password, "A password must be supplied." );
        try {
            log.debug( "Attempting manual authentication as " + userName + "." );
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication( manualAuthenticationService.attemptAuthentication( userName, password ) );
            return securityContext;
        } finally {
            log.debug( "Erasing credentials for manual authentication." );
            this.userName = null;
            this.password = null;
        }
    }
}
