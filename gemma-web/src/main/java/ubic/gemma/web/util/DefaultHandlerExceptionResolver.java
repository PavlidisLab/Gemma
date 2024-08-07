package ubic.gemma.web.util;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import java.util.Arrays;
import java.util.Properties;

/**
 * Custom exception resolver for Gemma.
 * <p>
 * This extends {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver} with additional
 * handling of exceptions.
 */
public class DefaultHandlerExceptionResolver extends HandlerExceptionResolverComposite implements HandlerExceptionResolver {

    private static final String HANDLED_EXCEPTION_LOGGER_NAME = "ubic.gemma.web.loggers.HandledException";
    private static final String UNHANDLED_EXCEPTION_LOGGER_NAME = "ubic.gemma.web.loggers.UnhandledException";

    public DefaultHandlerExceptionResolver() {
        setExceptionResolvers( Arrays.asList(
                springExceptionResolver(),
                simpleMappingExceptionResolver(),
                clientAbortExceptionResolver(),
                unhandledExceptionResolver() ) );
    }

    /**
     * For all Spring-specific exceptions.
     */
    private org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver springExceptionResolver() {
        org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver resolver = new org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver();
        resolver.setWarnLogCategory( HANDLED_EXCEPTION_LOGGER_NAME );
        return resolver;
    }

    /**
     * Handle common exceptions used in Gemma Web.
     */
    private SimpleMappingExceptionResolver simpleMappingExceptionResolver() {
        SimpleMappingExceptionResolver resolver = new SimpleMappingExceptionResolver();
        resolver.addStatusCode( "error/400", 400 );
        resolver.addStatusCode( "error/403", 403 );
        resolver.addStatusCode( "error/404", 404 );
        resolver.addStatusCode( "error/503", 503 );
        Properties mappings = new Properties();
        mappings.setProperty( AccessDeniedException.class.getName(), "error/403" );
        mappings.setProperty( EntityNotFoundException.class.getName(), "error/404" );
        mappings.setProperty( ServiceUnavailableException.class.getName(), "error/503" );
        mappings.setProperty( IllegalArgumentException.class.getName(), "error/400" );
        resolver.setExceptionMappings( mappings );
        resolver.setWarnLogCategory( HANDLED_EXCEPTION_LOGGER_NAME );
        return resolver;
    }

    /**
     * This is specific to Tomcat, it produces an empty view when a client aborts the HTTP connection.
     */
    private ClientAbortExceptionResolver clientAbortExceptionResolver() {
        ClientAbortExceptionResolver resolver = new ClientAbortExceptionResolver();
        resolver.setWarnLogCategory( HANDLED_EXCEPTION_LOGGER_NAME );
        return resolver;
    }

    /**
     * Covers all unhandled exception by logging them and producing a 500 error.
     */
    private UnhandledExceptionResolver unhandledExceptionResolver() {
        UnhandledExceptionResolver resolver = new UnhandledExceptionResolver();
        resolver.setErrorCategory( UNHANDLED_EXCEPTION_LOGGER_NAME );
        resolver.setStatusCode( 500 );
        resolver.setErrorView( "error/500" );
        return resolver;
    }
}
