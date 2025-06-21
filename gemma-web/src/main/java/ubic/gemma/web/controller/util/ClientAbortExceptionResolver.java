package ubic.gemma.web.controller.util;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Resolves a ClientAbortException to an empty view.
 * <p>
 * This is only applicable to Tomcat.
 * @author poirigui
 */
public class ClientAbortExceptionResolver extends AbstractHandlerExceptionResolver {

    @Override
    protected ModelAndView doResolveException( HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex ) {
        if ( "org.apache.catalina.connector.ClientAbortException".equals( ex.getClass().getName() ) ) {
            return new ModelAndView();
        }
        return null;
    }
}
