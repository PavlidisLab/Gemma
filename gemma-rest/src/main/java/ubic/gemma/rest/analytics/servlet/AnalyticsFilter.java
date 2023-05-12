package ubic.gemma.rest.analytics.servlet;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * @author poirigui
 */
public class AnalyticsFilter extends OncePerRequestFilter {

    private AnalyticsProvider analyticsProvider;

    private String eventName;

    public AnalyticsFilter() {
        addRequiredProperty( "eventName" );
    }

    @Override
    protected void initFilterBean() {
        Assert.isTrue( !StringUtils.isEmpty( eventName ), "The eventName must be set to a non-empty string." );
        analyticsProvider = WebApplicationContextUtils.getRequiredWebApplicationContext( getServletContext() )
                .getBean( AnalyticsProvider.class );
    }

    /**
     * Set the event name used for reporting analytics.
     */
    public void setEventName( String eventName ) {
        this.eventName = eventName;
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain chain ) throws ServletException, IOException {
        Date date = new Date();
        chain.doFilter( request, response );
        try {
            RequestContextHolder.setRequestAttributes( new ServletRequestAttributes( request ) );
            analyticsProvider.sendEvent( eventName, date,
                    "method", request.getMethod(),
                    "endpoint", request.getRequestURI() );
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
