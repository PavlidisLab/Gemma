package ubic.gemma.security.authentication;

import java.io.IOException;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import ubic.gemma.util.JSONUtil;

public class AjaxAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess( HttpServletRequest request, HttpServletResponse response,
            Authentication authentication ) throws ServletException, IOException {
        
                
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

            super.onAuthenticationSuccess( request, response, authentication );
            authentication.getName();
            
            jsonText = "{success:true,user:\'"+ authentication.getName()+"\'}";
            jsonUtil.writeToResponse( jsonText);
        } else {

            this.setRedirectStrategy( new DefaultRedirectStrategy() );

            super.onAuthenticationSuccess( request, response, authentication );
        }

    }

}
