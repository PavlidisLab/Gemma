/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.common.auditAndSecurity.recaptcha;

import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class ReCaptcha {

    public static final String HOST = "https://www.google.com";
    public static final String PATH = "/recaptcha/api/siteverify";
    public static final String URL = HOST + PATH;

    private static final String POST_PARAM_SECRET = "secret";
    private static final String POST_PARAM_RESPONSE = "response";
    private static final String POST_PARAM_IP = "remoteip";

    private String privateKey;

    public ReCaptcha( String privateKey ) {
        this.privateKey = privateKey;
    }

    public boolean isPrivateKeySet() {
        return this.privateKey != null && !this.privateKey.isEmpty();
    }

    public ReCaptchaResponse validateRequest( HttpServletRequest request ) {
        String response = SimpleHttp.get( URL, createUrlParameters( request.getParameter( "g-recaptcha-response" ), request.getRemoteAddr() ) );

        JSONObject responseJSON = JSONObject.fromObject( response );

        if ( responseJSON.getBoolean( "success" ) ) {
            return new ReCaptchaResponse( true, "" );
        } else {
            return new ReCaptchaResponse( false, "unknown error." );
        }
    }

    private String createUrlParameters( String response, String remoteAddr ) {
        return POST_PARAM_SECRET + "=" + privateKey + "&" +
                POST_PARAM_RESPONSE + "=" + response + "&" +
                POST_PARAM_IP + "=" + remoteAddr;
    }

}
