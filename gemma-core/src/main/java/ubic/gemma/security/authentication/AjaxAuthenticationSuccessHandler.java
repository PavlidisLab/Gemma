package ubic.gemma.security.authentication;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

public class AjaxAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess( HttpServletRequest request, HttpServletResponse response,
            Authentication authentication ) throws ServletException, IOException {

        String ajaxLoginTrue = request.getParameter( "ajaxLoginTrue" );

        if ( ajaxLoginTrue != null && ajaxLoginTrue.equals( "true" ) ) {

            this.setRedirectStrategy( new RedirectStrategy() {

                @Override
                public void sendRedirect( HttpServletRequest request, HttpServletResponse response, String s )
                        throws IOException {
                    // do nothing, no redirect to make it work with extjs

                }
            } );

            super.onAuthenticationSuccess( request, response, authentication );

            HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper( response );

            Writer out = responseWrapper.getWriter();

            out.write( "{success:true}" );
            out.flush();
            out.close();

        } else {

            this.setRedirectStrategy( new DefaultRedirectStrategy() );

            super.onAuthenticationSuccess( request, response, authentication );
        }

    }

}
