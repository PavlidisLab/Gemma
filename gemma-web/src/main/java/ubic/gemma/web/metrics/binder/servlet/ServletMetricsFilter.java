package ubic.gemma.web.metrics.binder.servlet;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.DefaultHttpServletRequestTagsProvider;
import io.micrometer.core.instrument.binder.http.HttpServletRequestTagsProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Metrics for servlet requests.
 * <p>
 * We attempt to provide tags that are as close as possible to Spring Boot's WebMvcTags.
 *
 * @author poirigui
 */
public class ServletMetricsFilter extends OncePerRequestFilter {

    private final HttpServletRequestTagsProvider tagsProvider = new DefaultHttpServletRequestTagsProvider();

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( request.getServletContext() );

        MeterRegistry registry;
        try {
            registry = ctx.getBean( MeterRegistry.class );
        } catch ( NoSuchBeanDefinitionException e ) {
            filterChain.doFilter( request, response );
            return;
        }

        Exception exception = null;
        Timer.Sample timerSample = Timer.start( registry );
        try {
            filterChain.doFilter( request, response );
        } catch ( Exception e ) {
            exception = e;
        } finally {
            timerSample.stop( registry.timer( "httpServlet", getTags( request, response, exception ) ) );
        }
    }

    private Iterable<Tag> getTags( HttpServletRequest request, HttpServletResponse response, Exception exception ) {
        return Tags.of( tagsProvider.getTags( request, response ) )
                .and( "uri", getRequestPath( request ) )
                .and( "exception", getException( exception ) );
    }

    private static String getRequestPath( HttpServletRequest request ) {
        String path = request.getServletPath();
        if ( request.getPathInfo() != null ) {
            path += request.getPathInfo();
        }
        return path;
    }

    private static String getException( Exception e ) {
        return e == null ? "None" : e.getClass().getName();
    }
}
