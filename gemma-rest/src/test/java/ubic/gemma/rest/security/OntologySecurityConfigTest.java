package ubic.gemma.rest.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ubic.gemma.core.context.TestComponent;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots only the {@code /ont/**} chain and checks what the servlet behind it sees.
 * <p>
 * Without this chain, the TGFVO services, which are {@code @Secured("IS_AUTHENTICATED_ANONYMOUSLY")}, find no
 * authentication at all and fail every request.
 */
class OntologySecurityConfigTest {

    @Configuration
    @TestComponent
    @EnableWebSecurity
    @Import(OntologySecurityConfig.class)
    static class Config {
    }

    private AnnotationConfigWebApplicationContext ctx;
    private FilterChainProxy filterChainProxy;

    @BeforeEach
    void setUp() {
        ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext( new MockServletContext() );
        ctx.register( Config.class );
        ctx.refresh();
        filterChainProxy = ctx.getBean( "springSecurityFilterChain", FilterChainProxy.class );
    }

    @AfterEach
    void tearDown() {
        ctx.close();
        SecurityContextHolder.clearContext();
    }

    private Authentication authenticationSeenBy( String servletPath, String pathInfo, String authorization ) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest( "GET", servletPath + pathInfo );
        req.setServletPath( servletPath );
        req.setPathInfo( pathInfo );
        if ( authorization != null ) {
            req.addHeader( "Authorization", authorization );
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<Authentication> seen = new AtomicReference<>();
        FilterChain servlet = ( request, response ) -> seen.set( SecurityContextHolder.getContext().getAuthentication() );
        filterChainProxy.doFilter( req, res, servlet );
        assertThat( res.getStatus() ).isEqualTo( 200 );
        return seen.get();
    }

    @Test
    void ontologyRequestsRunAsAnonymous() throws Exception {
        Authentication auth = authenticationSeenBy( "/ont", "/TGFVO/12", null );
        assertThat( auth ).isInstanceOf( AnonymousAuthenticationToken.class );
        assertThat( auth.getAuthorities() ).extracting( GrantedAuthority::getAuthority )
                .containsExactly( "IS_AUTHENTICATED_ANONYMOUSLY" );
    }

    @Test
    void credentialsAreIgnored() throws Exception {
        // the pages are the same public view for everyone, so no login is attempted
        Authentication auth = authenticationSeenBy( "/ont", "/TGFVO/12", "Basic dXNlcjpwYXNzd29yZA==" );
        assertThat( auth ).isInstanceOf( AnonymousAuthenticationToken.class );
    }

    @Test
    void otherPathsAreNotMatched() throws Exception {
        assertThat( authenticationSeenBy( "/rest/v2", "/datasets", null ) ).isNull();
    }
}
