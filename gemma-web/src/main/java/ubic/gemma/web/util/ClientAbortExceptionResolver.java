package ubic.gemma.web.util;

import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Resolves a ClientAbortException to an empty view.
 * <p>
 * This is only applicable to Tomcat.
 * @author poirigui
 */
public class ClientAbortExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException( HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex ) {
        if ( "org.apache.catalina.connector.ClientAbortException".equals( ex.getClass().getName() ) ) {
            return new ModelAndView();
        }
        return null;
    }
}
