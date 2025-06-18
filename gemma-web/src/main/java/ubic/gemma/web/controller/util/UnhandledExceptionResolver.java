package ubic.gemma.web.controller.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Resolver used when no other resolver can intervene.
 * <p>
 * This is essentially delegating work to a {@link SimpleMappingExceptionResolver} with the added benefit that the
 * exception can be logged beforehand with a given error category.
 * @author poirigui
 * @see SimpleMappingExceptionResolver
 */
public class UnhandledExceptionResolver implements HandlerExceptionResolver {

    private final SimpleMappingExceptionResolver delegate = new SimpleMappingExceptionResolver() {{
        setDefaultStatusCode( 500 );
    }};

    @Nullable
    private Log errorLogger = null;

    @Override
    public ModelAndView resolveException( HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex ) {
        if ( this.errorLogger != null && this.errorLogger.isErrorEnabled() ) {
            this.errorLogger.error( "An unhandled exception was intercepted: " + ExceptionUtils.getRootCauseMessage( ex ), ex );
        }
        return delegate.resolveException( request, response, handler, ex );
    }

    /**
     * Set the name of the logger to use for reporting unhandled exceptions.
     * <p>
     * By default, no logging is done.
     * @see org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver#setWarnLogCategory(String)
     */
    public void setErrorCategory( @Nullable String loggerName ) {
        this.errorLogger = LogFactory.getLog( loggerName );
    }

    /**
     * Set the status code to use for reporting unhandled exception.
     * <p>
     * Defaults to 500.
     * @see SimpleMappingExceptionResolver#setDefaultStatusCode(int)
     */
    public void setStatusCode( int statusCode ) {
        this.delegate.setDefaultStatusCode( statusCode );
    }

    /**
     * Set the view to use for reporting unhandled exception.
     * <p>
     * Default is none.
     * @see SimpleMappingExceptionResolver#setDefaultErrorView(String)
     */
    public void setErrorView( String errorView ) {
        this.delegate.setDefaultErrorView( errorView );
    }
}
