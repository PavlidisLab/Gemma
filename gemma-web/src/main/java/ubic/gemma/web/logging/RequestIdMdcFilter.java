package ubic.gemma.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Per-request filter that populates two MDC keys for the duration of the request:
 * <ul>
 *     <li>{@code requestId} — the inbound {@code X-Request-Id} header if present, otherwise a freshly
 *         generated UUID. The resolved value is also echoed back on the response so callers and log
 *         shippers can correlate a single line in {@code gemma.log} to an HTTP exchange.</li>
 *     <li>{@code userId} — the authenticated principal name from {@link SecurityContextHolder}, or
 *         {@code "anonymous"} if no authentication is in scope.</li>
 * </ul>
 *
 * <p>The filter relies on log4j's {@link CloseableThreadContext} (== SLF4J MDC) so the keys are
 * automatically removed on exit even if the downstream chain throws. Cross-thread propagation of
 * these keys onto async executors is already handled by the {@code DelegatingThreadContext*}
 * wrappers in {@code ubic.gemma.core.logging.log4j} (wired by
 * {@code TaskExecutorThreadContextInheritPostProcessor}); this filter is therefore the only piece
 * required to make request-scoped log enrichment live end-to-end.
 *
 * <p>Filter ordering: must run <em>after</em> Spring Security's filter chain has populated
 * {@link SecurityContextHolder} for {@code userId} to resolve to the real user. In the gemma-web
 * {@code web.xml} the filter-mapping for this filter is declared after
 * {@code springSecurityFilterChain}, which is the servlet-spec way of saying "later in the chain".
 *
 * <p>See {@code LOGGING_MODERNIZATION_RECCE.md} §3 Phase 2 for context.
 *
 * @author Gemma
 */
public class RequestIdMdcFilter extends OncePerRequestFilter {

    /** Inbound / outbound header carrying the request correlation id. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** MDC key for the per-request correlation id. */
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    /** MDC key for the authenticated principal name. */
    public static final String USER_ID_MDC_KEY = "userId";

    /** Placeholder principal name written into MDC when no authentication is in scope. */
    public static final String ANONYMOUS_USER = "anonymous";

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
        String requestId = resolveRequestId( request );
        String userId = resolveUserId();

        // Echo the resolved request id onto the response so external callers can correlate a
        // support ticket / network trace to a single log line without ops involvement.
        response.setHeader( REQUEST_ID_HEADER, requestId );

        try ( CloseableThreadContext.Instance ignored = CloseableThreadContext
                .put( REQUEST_ID_MDC_KEY, requestId )
                .put( USER_ID_MDC_KEY, userId ) ) {
            filterChain.doFilter( request, response );
        }
    }

    private static String resolveRequestId( HttpServletRequest request ) {
        String header = request.getHeader( REQUEST_ID_HEADER );
        if ( header != null && !header.isEmpty() ) {
            return header;
        }
        return UUID.randomUUID().toString();
    }

    private static String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication == null || !authentication.isAuthenticated() ) {
            return ANONYMOUS_USER;
        }
        String name = authentication.getName();
        if ( name == null || name.isEmpty() ) {
            return ANONYMOUS_USER;
        }
        return name;
    }
}
