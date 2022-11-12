package ubic.gemma.core.util.test;

import gemma.gsec.AuthorityConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.gemma.core.security.authentication.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for manipulating the {@link SecurityContextHolder} in a test envirnoment.
 * @author poirigui
 */
@Service
@CommonsLog
public class TestAuthenticationUtilsImpl implements InitializingBean, TestAuthenticationUtils {

    @Autowired
    private UserManager userManager;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public void afterPropertiesSet() {
        if ( authenticationManager instanceof ProviderManager ) {
            ( ( ProviderManager ) authenticationManager ).getProviders().add( new TestingAuthenticationProvider() );
        } else {
            log.warn( String.format( "The authenticationManager bean is not implemented with %s, the testing authentication provider will not be registered.",
                    ProviderManager.class.getName() ) );
        }
    }

    /**
     * Elevate to administrative privileges (tests normally run this way, this can be used to set it back if you called
     * runAsUser). This gets called before each test, no need to run it yourself otherwise.
     */
    @Override
    public void runAsAdmin() {
        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) } ) );
        token.setAuthenticated( true );
        createAndSetSecurityContext( token );
    }

    /**
     * Run as a regular user.
     *
     * @param userName user name
     */
    @Override
    public void runAsUser( String userName ) {
        UserDetails user = userManager.loadUserByUsername( userName );
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>( user.getAuthorities() );
        TestingAuthenticationToken token = new TestingAuthenticationToken( userName, "testing", grantedAuthorities );
        token.setAuthenticated( true );
        createAndSetSecurityContext( token );
    }

    @Override
    public void runAsAnonymous() {
        TestingAuthenticationToken token = new TestingAuthenticationToken( AuthorityConstants.ANONYMOUS_USER_NAME, null,
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ANONYMOUS_GROUP_AUTHORITY ) } ) );
        token.setAuthenticated( false );
        createAndSetSecurityContext( token );
    }

    private static void createAndSetSecurityContext( TestingAuthenticationToken token ) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication( token );
        SecurityContextHolder.setContext( securityContext );
    }
}
