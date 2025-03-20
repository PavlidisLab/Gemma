package ubic.gemma.core.util.test;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.authentication.UserDetailsImpl;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;

import java.util.*;

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

    @Value("${gemma.anonymousAuth.key}")
    private String anonymousAuthKey;

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
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator",
                "administrator", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        createAndSetSecurityContext( token );
    }

    @Override
    public void runAsAgent() {
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator",
                "administrator", AuthorityConstants.AGENT_GROUP_AUTHORITY );
        createAndSetSecurityContext( token );
    }

    /**
     * Run as a regular user.
     *
     * @param userName        user name
     * @param createIfMissing
     */
    @Override
    public void runAsUser( String userName, boolean createIfMissing ) {
        UserDetails user;
        try {
            user = this.userManager.loadUserByUsername( userName );
        } catch ( UsernameNotFoundException e ) {
            if ( createIfMissing ) {
                user = new UserDetailsImpl( "foo", userName, true, Collections.singletonList( new SimpleGrantedAuthority( AuthorityConstants.USER_GROUP_AUTHORITY ) ),
                        RandomStringUtils.randomAlphabetic( 10 ) + "@example.com", "key", new Date() );
                this.userManager.createUser( user );
            } else {
                throw e;
            }
        }
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>( user.getAuthorities() );
        TestingAuthenticationToken token = new TestingAuthenticationToken( userName, "testing", grantedAuthorities );
        token.setAuthenticated( true );
        createAndSetSecurityContext( token );
    }

    @Override
    public void runAsAnonymous() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken( anonymousAuthKey, "anonymousUser",
                Arrays.asList( new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ) );
        createAndSetSecurityContext( token );
    }

    private static void createAndSetSecurityContext( Authentication token ) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication( token );
        SecurityContextHolder.setContext( securityContext );
    }
}
