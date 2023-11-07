package ubic.gemma.web.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Resolver used when no other resolver can intervene.
 */
@CommonsLog
public class UnhandledExceptionResolver implements HandlerExceptionResolver {

    private final SimpleMappingExceptionResolver delegate = new SimpleMappingExceptionResolver();
    private String errorView;

    @Override
    public ModelAndView resolveException( HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex ) {
        Assert.notNull( errorView, "An error view must be set." );
        log.error( "An unhandled exception was intercepted: " + ExceptionUtils.getRootCauseMessage( ex ), ex );
        return delegate.resolveException( request, response, handler, ex );
    }

    public void setErrorView( String errorView ) {
        this.errorView = errorView;
        this.delegate.setDefaultErrorView( errorView );
    }
}
