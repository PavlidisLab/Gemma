package ubic.gemma.core.util.test;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.authentication.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public final class AuthenticationTestingUtilImpl implements AuthenticationTestingUtil {

    private final UserManager userManager;

    @Autowired
    public AuthenticationTestingUtilImpl( UserManager userManager ) {
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
    public void grantAdminAuthority( ApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                Arrays.asList( new GrantedAuthority[] {
                        new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) } ) );

        token.setAuthenticated( true );

        AuthenticationTestingUtilImpl.putTokenInContext( token );
    }

    @Override
    public void logOut( ApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
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
    public void switchToUser( ApplicationContext ctx, String username ) {

        UserDetails user = userManager.loadUserByUsername( username );

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>( user.getAuthorities() );

        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        TestingAuthenticationToken token = new TestingAuthenticationToken( username, "testing", grantedAuthorities );
        token.setAuthenticated( true );

        AuthenticationTestingUtilImpl.putTokenInContext( token );
    }
}
