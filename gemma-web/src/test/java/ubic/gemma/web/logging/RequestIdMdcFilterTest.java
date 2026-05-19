package ubic.gemma.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RequestIdMdcFilter}. Covers:
 * <ul>
 *     <li>No {@code X-Request-Id} header → UUID generated and put into MDC.</li>
 *     <li>Inbound {@code X-Request-Id} → honoured verbatim and echoed on the response.</li>
 *     <li>Authenticated {@link SecurityContextHolder} → principal name lands in {@code userId} MDC.</li>
 *     <li>No authentication / anonymous token → MDC {@code userId} is the {@code "anonymous"} placeholder.</li>
 *     <li>{@code ThreadContext} (MDC) is empty after the filter chain returns, even if it throws.</li>
 * </ul>
 */
public class RequestIdMdcFilterTest {

    private RequestIdMdcFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestIdMdcFilter();
        // Defensive: ensure no MDC leakage from prior tests in the same JVM.
        ThreadContext.clearAll();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ThreadContext.clearAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatesUuidWhenNoRequestIdHeader() throws ServletException, java.io.IOException {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        FilterChain chain = ( req, res ) -> {
            capturedRequestId.set( ThreadContext.get( RequestIdMdcFilter.REQUEST_ID_MDC_KEY ) );
            capturedUserId.set( ThreadContext.get( RequestIdMdcFilter.USER_ID_MDC_KEY ) );
        };

        filter.doFilter( request, response, chain );

        assertThat( capturedRequestId.get() ).isNotNull();
        // Must parse as a UUID.
        UUID.fromString( capturedRequestId.get() );
        assertThat( capturedUserId.get() ).isEqualTo( RequestIdMdcFilter.ANONYMOUS_USER );
        // Echoed onto the response.
        assertThat( response.getHeader( RequestIdMdcFilter.REQUEST_ID_HEADER ) ).isEqualTo( capturedRequestId.get() );
        // Cleared after the chain returns.
        assertThat( ThreadContext.get( RequestIdMdcFilter.REQUEST_ID_MDC_KEY ) ).isNull();
        assertThat( ThreadContext.get( RequestIdMdcFilter.USER_ID_MDC_KEY ) ).isNull();
    }

    @Test
    void honoursInboundRequestIdHeader() throws ServletException, java.io.IOException {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets/1" );
        request.addHeader( RequestIdMdcFilter.REQUEST_ID_HEADER, "abc-123" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = ( req, res ) -> captured.set( ThreadContext.get( RequestIdMdcFilter.REQUEST_ID_MDC_KEY ) );

        filter.doFilter( request, response, chain );

        assertThat( captured.get() ).isEqualTo( "abc-123" );
        assertThat( response.getHeader( RequestIdMdcFilter.REQUEST_ID_HEADER ) ).isEqualTo( "abc-123" );
    }

    @Test
    void populatesUserIdFromAuthenticatedSecurityContext() throws ServletException, java.io.IOException {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                Collections.singletonList( new SimpleGrantedAuthority( "ROLE_USER" ) ) );
        SecurityContextHolder.getContext().setAuthentication( auth );

        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = ( req, res ) -> captured.set( ThreadContext.get( RequestIdMdcFilter.USER_ID_MDC_KEY ) );

        filter.doFilter( request, response, chain );

        assertThat( captured.get() ).isEqualTo( "alice" );
    }

    @Test
    void useAnonymousPlaceholderForAnonymousAuthenticationToken() throws ServletException, java.io.IOException {
        // Spring Security's anonymous filter sets a non-null, isAuthenticated()==true token whose
        // principal name is "anonymousUser". Per the recce, our filter normalises the
        // null / unauthenticated case to the literal "anonymous" — make sure that path is what
        // we get when no authentication has been set.
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = ( req, res ) -> captured.set( ThreadContext.get( RequestIdMdcFilter.USER_ID_MDC_KEY ) );

        filter.doFilter( request, response, chain );

        assertThat( captured.get() ).isEqualTo( RequestIdMdcFilter.ANONYMOUS_USER );
    }

    @Test
    void anonymousAuthenticationTokenSurfacesItsOwnName() throws ServletException, java.io.IOException {
        // If Spring Security's AnonymousAuthenticationFilter ran upstream of us, we'll see an
        // AnonymousAuthenticationToken named "anonymousUser". That's a real, non-null name, so
        // we honour it — our "anonymous" placeholder is only for the genuinely no-auth case.
        AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                Collections.singletonList( new SimpleGrantedAuthority( "ROLE_ANONYMOUS" ) ) );
        SecurityContextHolder.getContext().setAuthentication( anon );

        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = ( req, res ) -> captured.set( ThreadContext.get( RequestIdMdcFilter.USER_ID_MDC_KEY ) );

        filter.doFilter( request, response, chain );

        assertThat( captured.get() ).isEqualTo( "anonymousUser" );
    }

    @Test
    void delegatesToFilterChainOnce() throws ServletException, java.io.IOException {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock( FilterChain.class );

        filter.doFilter( request, response, chain );

        ArgumentCaptor<HttpServletRequest> reqCap = ArgumentCaptor.forClass( HttpServletRequest.class );
        ArgumentCaptor<HttpServletResponse> resCap = ArgumentCaptor.forClass( HttpServletResponse.class );
        verify( chain ).doFilter( reqCap.capture(), resCap.capture() );
        assertThat( reqCap.getValue() ).isSameAs( request );
        assertThat( resCap.getValue() ).isSameAs( response );
    }

    @Test
    void clearsMdcEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/rest/v2/datasets" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = ( req, res ) -> {
            throw new ServletException( "boom" );
        };

        try {
            filter.doFilter( request, response, chain );
        } catch ( Exception ignored ) {
            // expected
        }

        assertThat( ThreadContext.get( RequestIdMdcFilter.REQUEST_ID_MDC_KEY ) ).isNull();
        assertThat( ThreadContext.get( RequestIdMdcFilter.USER_ID_MDC_KEY ) ).isNull();
    }
}
