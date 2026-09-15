package ubic.gemma.rest.servlet;

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
 * Per-request filter that populates two MDC keys for the duration of a REST request:
 * <ul>
 *     <li>{@code requestId} — the inbound {@code X-Request-Id} header if present, otherwise a freshly
 *         generated UUID. Echoed back on the response.</li>
 *     <li>{@code userId} — the authenticated principal name from {@link SecurityContextHolder}, or
 *         {@code "anonymous"} when no authentication is in scope.</li>
 * </ul>
 *
 * <p>This is the gemma-rest module's counterpart to
 * {@code ubic.gemma.web.logging.RequestIdMdcFilter} (gemma-web). The two classes are deliberately
 * separate because gemma-rest does not depend on gemma-web. When running inside gemma-web's WAR,
 * the gemma-web copy is registered in {@code gemma-web/WEB-INF/web.xml} and is the one that fires
 * for Jersey requests (Jersey is mounted under the same web.xml). When gemma-rest is built as a
 * standalone WAR (the {@code gemma-rest-war} profile of {@code GEMMA_REST_STANDALONE_ROADMAP.md}),
 * this copy is the one registered in {@code gemma-rest/WEB-INF/web.xml}.
 *
 * <p>See {@code LOGGING_MODERNIZATION_RECCE.md} §3 Phase 2 for context. ThreadContext propagation
 * across async executors is already handled by the {@code DelegatingThreadContext*} layer in
 * {@code ubic.gemma.core.logging.log4j}.
 */
public class RequestIdMdcFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String ANONYMOUS_USER = "anonymous";

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
        String requestId = resolveRequestId( request );
        String userId = resolveUserId();

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
