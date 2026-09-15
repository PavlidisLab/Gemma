package ubic.gemma.rest.servlet;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rewrites the path to the index file.
 * <p>
 * The filter sets {@code Content-Type} itself, because the forward loses it. Tomcat's DefaultServlet
 * emits the file with no {@code Content-Type} header at all when it is reached through a FORWARD
 * dispatch: the direct URL {@code /resources/restapidocs/index.html} comes back as
 * {@code text/html; charset=UTF-8}, while the forwarded directory URL comes back with byte-identical
 * content and no such header. The deployment sends {@code X-Content-Type-Options: nosniff}, so a
 * typeless response may not be sniffed and the browser renders the Swagger UI page as plain text.
 * Setting the type here also survives the forward, since DefaultServlet skips its own
 * {@code setContentType} when one is already set.
 *
 * @author poirigui
 */
public class RestapidocsIndexRewriteFilter extends OncePerRequestFilter {

    /**
     * Match the main request to the resource, which is redispatched with the index.html.
     */
    private static final RequestMatcher REQUEST_MATCHER = new AntPathRequestMatcher( "/resources/restapidocs/" );

    /**
     * Match requests with missing slash, which are redirected appropriately.
     */
    private static final RequestMatcher REQUEST_WITH_MISSING_SLASH_MATCHER = new AntPathRequestMatcher( "/resources/restapidocs" );

    /**
     * Content type of the index page, set explicitly because the forward loses it.
     */
    static final String INDEX_CONTENT_TYPE = "text/html;charset=UTF-8";

    @Override
    public void doFilterInternal( HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain ) throws IOException, ServletException {
        if ( REQUEST_MATCHER.matches( servletRequest ) ) {
            servletResponse.setContentType( INDEX_CONTENT_TYPE );
            servletRequest.getRequestDispatcher( "/resources/restapidocs/index.html" )
                    .forward( servletRequest, servletResponse );
            return;
        }

        if ( REQUEST_WITH_MISSING_SLASH_MATCHER.matches( servletRequest ) ) {
            String redirectUrl = ServletUriComponentsBuilder.fromRequest( servletRequest )
                    .scheme( null ).host( null ).port( -1 )
                    .replacePath( servletRequest.getContextPath() + "/resources/restapidocs/" )
                    .build()
                    .toString();
            servletResponse.sendRedirect( redirectUrl );
            return;
        }
        filterChain.doFilter( servletRequest, servletResponse );
    }
}
