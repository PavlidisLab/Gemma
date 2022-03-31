package ubic.gemma.core.util.test;

import gemma.gsec.AuthorityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ubic.gemma.core.security.authentication.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public final class AuthenticationTestingUtilImpl implements AuthenticationTestingUtil {


    private final ProviderManager providerManager;
    private final UserManager userManager;

    @Autowired
    public AuthenticationTestingUtilImpl( @Qualifier("authenticationManager") AuthenticationManager providerManager, UserManager userManager ) {
        this.providerManager = ( ProviderManager ) providerManager;
        this.userManager = userManager;
    }

    private static void putTokenInContext( AbstractAuthenticationToken token ) {
        SecurityContextHolder.getContext().setAuthentication( token );
    }

    /**
     * Grant authority to a test user, with admin privileges, and put the token in the context. This means your tests
     * will be authorized to do anything an administrator would be able to do.
     *
     * @param ctx context
     */
    @Override
    public void grantAdminAuthority() {
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) } ) );

        token.setAuthenticated( true );

        AuthenticationTestingUtilImpl.putTokenInContext( token );
    }

    @Override
    public void logOut() {
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        TestingAuthenticationToken token = new TestingAuthenticationToken( AuthorityConstants.ANONYMOUS_USER_NAME, null,
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ANONYMOUS_GROUP_AUTHORITY ) } ) );

        token.setAuthenticated( false );

        AuthenticationTestingUtilImpl.putTokenInContext( token );
    }

    /**
     * Grant authority to a test user, with regular user privileges, and put the token in the context. This means your
     * tests will be authorized to do anything that user could do
     *
     * @param ctx      context
     * @param username user name
     */
    @Override
    public void switchToUser( String username ) {

        UserDetails user = userManager.loadUserByUsername( username );

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>( user.getAuthorities() );

        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        TestingAuthenticationToken token = new TestingAuthenticationToken( username, "testing", grantedAuthorities );
        token.setAuthenticated( true );

        AuthenticationTestingUtilImpl.putTokenInContext( token );
    }
}
