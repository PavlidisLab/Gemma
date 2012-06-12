/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.login;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import ubic.gemma.util.JSONUtil;

/**
 * Strategy used to handle a failed user authentication
 * if it is a ajax style login (ajaxLoginTrue parameter = true) then no redirect happens and a some JSON is sent to the client
 * 
 * if the request is not ajax style then the default redirection takes place
 * 
 * configuration in applicationContext-security.xml
 * 
 * @author cmcdonald
 * @version $Id$
 */
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
                public void sendRedirect( HttpServletRequest re, HttpServletResponse res, String s ) {
                    // do nothing, no redirect to make it work with extjs

                }
            } );

            super.onAuthenticationFailure( request, response, exception );

            jsonText = "{success:false}";
            jsonUtil.writeToResponse( jsonText );

        }

        else {

            this.setRedirectStrategy( new DefaultRedirectStrategy() );

            super.onAuthenticationFailure( request, response, exception );

        }

    }

}
