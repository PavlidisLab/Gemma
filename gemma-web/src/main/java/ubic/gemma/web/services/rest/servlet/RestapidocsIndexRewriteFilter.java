package ubic.gemma.web.services.rest.servlet;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rewrites the path to the index file.
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

    @Override
    public void doFilterInternal( HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain ) throws IOException, ServletException {
        if ( REQUEST_MATCHER.matches( servletRequest ) ) {
            servletRequest.getRequestDispatcher( "/resources/restapidocs/index.html" )
                    .forward( servletRequest, servletResponse );
            return;
        }

        if ( REQUEST_WITH_MISSING_SLASH_MATCHER.matches( servletRequest ) ) {
            String redirectUrl = ServletUriComponentsBuilder.fromRequest( servletRequest )
                    .scheme( null ).host( null )
                    .replacePath( servletRequest.getContextPath() + "/resources/restapidocs/" )
                    .build()
                    .toString();
            servletResponse.sendRedirect( redirectUrl );
            return;
        }
        filterChain.doFilter( servletRequest, servletResponse );
    }
}
