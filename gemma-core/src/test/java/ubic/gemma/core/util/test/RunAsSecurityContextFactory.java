package ubic.gemma.core.util.test;

import gemma.gsec.AuthorityConstants;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;

/**
 * Creates a security context based on a {@link RunAs} annotation.
 * @author poirigui
 */
public class RunAsSecurityContextFactory implements WithSecurityContextFactory<RunAs> {

    @Override
    public SecurityContext createSecurityContext( RunAs annotation ) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        if ( annotation.value() == RunAs.Role.ADMIN ) {
            TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                    Arrays.asList( new GrantedAuthority[] {
                            new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) } ) );
            token.setAuthenticated( true );
            securityContext.setAuthentication( token );
        } else if ( annotation.value() == RunAs.Role.AGENT ) {
            TestingAuthenticationToken token = new TestingAuthenticationToken( "gemmaAgent", "administrator",
                    Arrays.asList( new GrantedAuthority[] {
                            new SimpleGrantedAuthority( AuthorityConstants.AGENT_GROUP_AUTHORITY ) } ) );
            token.setAuthenticated( true );
            securityContext.setAuthentication( token );
        } else if ( annotation.value() == RunAs.Role.ANONYMOUS ) {
            TestingAuthenticationToken token = new TestingAuthenticationToken( AuthorityConstants.ANONYMOUS_USER_NAME, null,
                    Arrays.asList( new GrantedAuthority[] {
                            new SimpleGrantedAuthority( AuthorityConstants.ANONYMOUS_GROUP_AUTHORITY ) } ) );
            token.setAuthenticated( false );
            securityContext.setAuthentication( token );
        }
        return securityContext;
    }
}
