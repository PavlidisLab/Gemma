package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ubic.gemma.web.listener.StartupListener;

/**
 * This class is used to reload the drop-downs initialized in the StartupListener. (from Appfuse)
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="reloadController" name="/reload.html"
 */
public class ReloadController implements Controller {
    private transient final Log log = LogFactory.getLog( UserListController.class );

    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( log.isDebugEnabled() ) {
            log.debug( "Entering 'execute' method" );
        }

        StartupListener.setupContext( request.getSession().getServletContext() );

        String referer = request.getHeader( "Referer" );

        if ( referer != null ) {
            log.info( "reload complete, reloading user back to: " + referer );
            List<Object> messages = new ArrayList<Object>();
            messages.add( "Reloading options completed successfully." );
            request.getSession().setAttribute( "messages", messages );
            response.sendRedirect( response.encodeRedirectURL( referer ) );
            return null;
        }
        response.setContentType( "text/html" );

        PrintWriter out = response.getWriter();

        out.println( "<html>" );
        out.println( "<head>" );
        out.println( "<title>Context Reloaded</title>" );
        out.println( "</head>" );
        out.println( "<body bgcolor=\"white\">" );
        out.println( "<script type=\"text/javascript\">" );
        out.println( "alert('Context Reload Succeeded! Click OK to continue.');" );
        out.println( "history.back();" );
        out.println( "</script>" );
        out.println( "</body>" );
        out.println( "</html>" );

        return null;
    }

}
