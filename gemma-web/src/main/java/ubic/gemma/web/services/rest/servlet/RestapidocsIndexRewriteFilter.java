package ubic.gemma.web.services.rest.servlet;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rewrites the path to the index file.
 * @author poirigui
 */
public class RestapidocsIndexRewriteFilter implements Filter {

    /**
     * Match the main request to the resource, which is redispatched with the index.html.
     */
    private static final RequestMatcher REQUEST_MATCHER = new AntPathRequestMatcher( "/resources/restapidocs/" );

    /**
     * Match requests with missing slash, which are redirected appropriately.
     */
    private static final RequestMatcher REQUEST_WITH_MISSING_SLASH_MATCHER = new AntPathRequestMatcher( "/resources/restapidocs" );

    @Override
    public void init( FilterConfig filterConfig ) throws ServletException {

    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain ) throws IOException, ServletException {
        if ( servletRequest instanceof HttpServletRequest ) {
            HttpServletRequest httpServletRequest = ( HttpServletRequest ) servletRequest;
            HttpServletResponse httpServletResponse = ( HttpServletResponse ) servletResponse;
            if ( REQUEST_MATCHER.matches( httpServletRequest ) ) {
                servletRequest.getRequestDispatcher( "/resources/restapidocs/index.html" )
                        .forward( servletRequest, servletResponse );
                return;
            }

            if ( REQUEST_WITH_MISSING_SLASH_MATCHER.matches( httpServletRequest ) ) {
                String redirectUrl = ServletUriComponentsBuilder.fromRequest( httpServletRequest )
                        .scheme( null ).host( null )
                        .replacePath( "/resources/restapidocs/" )
                        .build()
                        .toString();
                httpServletResponse.sendRedirect( redirectUrl );
                return;
            }
        }
        filterChain.doFilter( servletRequest, servletResponse );
    }

    @Override
    public void destroy() {

    }
}
