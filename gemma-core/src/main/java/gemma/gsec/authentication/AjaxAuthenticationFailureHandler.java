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
package gemma.gsec.authentication;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Strategy used to handle a failed user authentication if it is a ajax style login (ajaxLoginTrue parameter = true)
 * then no redirect happens and a some JSON is sent to the client if the request is not ajax style then the default
 * redirection takes place
 * <p>
 * This is in gemma-core because configuration takes place in applicationContext-security.xml
 *
 * @author cmcdonald
 * @version $Id: AjaxAuthenticationFailureHandler.java,v 1.9 2013/07/24 00:42:18 paul Exp $
 */
public class AjaxAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception ) throws ServletException, IOException {
        String ajaxLoginTrue = request.getParameter( "ajaxLoginTrue" );
        if ( ajaxLoginTrue != null && ajaxLoginTrue.equals( "true" ) ) {
            JSONObject json = new JSONObject();
            json.put( "success", false );
            response.setContentType( MediaType.APPLICATION_JSON_VALUE );
            response.sendError( HttpServletResponse.SC_UNAUTHORIZED, json.toString() );
        } else {
            super.onAuthenticationFailure( request, response, exception );
        }
    }

}
