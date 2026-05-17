package ubic.gemma.web.metrics.binder.servlet;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
// Phase 2: Micrometer's pre-jakarta http binder takes javax.servlet types; their jakarta-aware
// replacement landed under a different package (io.micrometer.observation). Until we migrate, we
// just don't attach the framework-provided tags - getRequestPath()/getException() below still apply.
// import io.micrometer.core.instrument.binder.http.DefaultHttpServletRequestTagsProvider;
// import io.micrometer.core.instrument.binder.http.HttpServletRequestTagsProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Metrics for servlet requests.
 * <p>
 * We attempt to provide tags that are as close as possible to Spring Boot's WebMvcTags.
 *
 * @author poirigui
 */
public class ServletMetricsFilter extends OncePerRequestFilter {

    // Phase 2: Micrometer's pre-jakarta http binder is dropped (see import comment above).

    private String metricName;

    public ServletMetricsFilter() {
        addRequiredProperty( "metricName" );
    }

    /**
     * Set the name under which servlet metrics are reported.
     */
    public void setMetricName( String metricName ) {
        this.metricName = metricName;
    }

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
            timerSample.stop( registry.timer( metricName, getTags( request, response, exception ) ) );
        }
    }

    private Iterable<Tag> getTags( HttpServletRequest request, HttpServletResponse response, Exception exception ) {
        return Tags.of(
                "method", request.getMethod(),
                "status", String.valueOf( response.getStatus() ),
                "uri", getRequestPath( request ),
                "exception", getException( exception ) );
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
