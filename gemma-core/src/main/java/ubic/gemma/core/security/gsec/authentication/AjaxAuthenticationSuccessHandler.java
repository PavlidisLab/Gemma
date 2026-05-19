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
package ubic.gemma.core.security.gsec.authentication;

import ubic.gemma.core.security.gsec.util.SecurityUtil;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Strategy used to handle a successful user authentication if it is a ajax style login (ajaxLoginTrue parameter = true)
 * then no redirect happens and a some JSON is sent to the client if the request is not ajax-style then the default
 * redirection takes place
 *
 * <p>
 * This is in gemma-core because configuration takes place in applicationContext-security.xml
 *
 * @author cmcdonald
 * @version $Id: AjaxAuthenticationSuccessHandler.java,v 1.12 2013/09/21 01:28:45 paul Exp $
 */
public class AjaxAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess( HttpServletRequest request, HttpServletResponse response,
        Authentication authentication ) throws ServletException, IOException {
        String ajaxLoginTrue = request.getParameter( "ajaxLoginTrue" );
        if ( ajaxLoginTrue != null && ajaxLoginTrue.equals( "true" ) ) {
            authentication.getName();
            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "user", authentication.getName() );
            json.put( "isAdmin", SecurityUtil.isUserAdmin() );
            String jsonText = json.toString();
            response.setContentType( MediaType.APPLICATION_JSON_VALUE );
            response.setContentLength( jsonText.length() );
            try ( Writer out = response.getWriter() ) {
                out.write( jsonText );
            }
        } else {
            super.onAuthenticationSuccess( request, response, authentication );
        }
    }

}
