package ubic.gemma.security.authentication;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import ubic.gemma.util.JSONUtil;

public class AjaxAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception ) throws ServletException, IOException {

        String ajaxLoginTrue = request.getParameter( "ajaxLoginTrue" );

        if ( ajaxLoginTrue != null && ajaxLoginTrue.equals( "true" ) ) {
            
            JSONUtil jsonUtil = new JSONUtil( request, response );
            String jsonText = null;

            this.setRedirectStrategy( new RedirectStrategy() {

                @Override
                public void sendRedirect( HttpServletRequest request, HttpServletResponse response, String s )
                        throws IOException {
                    // do nothing, no redirect to make it work with extjs

                }
            } );

            super.onAuthenticationFailure( request, response, exception );
            
            jsonText = "{success:false}";
            jsonUtil.writeToResponse( jsonText);
           
        }

        else {

            this.setRedirectStrategy( new DefaultRedirectStrategy() );

            super.onAuthenticationFailure( request, response, exception );

        }

    }

}
