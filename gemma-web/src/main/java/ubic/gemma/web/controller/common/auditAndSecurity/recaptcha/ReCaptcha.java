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

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.StringUtils.urlEncode;

public class ReCaptcha {

    public static final String URL = "https://www.google.com/recaptcha/api/siteverify";

    private static final String RESPONSE_REQUEST_PARAMETER = "g-recaptcha-response";
    private static final String POST_PARAM_SECRET = "secret";
    private static final String POST_PARAM_RESPONSE = "response";
    private static final String POST_PARAM_IP = "remoteip";

    private final String privateKey;

    public ReCaptcha( String privateKey ) {
        this.privateKey = privateKey;
    }

    public boolean isPrivateKeySet() {
        return this.privateKey != null && !this.privateKey.isEmpty();
    }

    public ReCaptchaResponse validateRequest( HttpServletRequest request ) throws ReCaptchaException {
        Assert.state( isPrivateKeySet(), "No private key is set, cannot validate reCaptcha." );
        try {
            String response = IOUtils.toString( new URL( URL + "?" + createUrlParameters( request ) ), StandardCharsets.UTF_8 );
            JSONObject responseJSON = new JSONObject( response );
            return new ReCaptchaResponse( isValid( responseJSON ), formatErrorCodes( responseJSON ) );
        } catch ( IOException e ) {
            throw new ReCaptchaException( "I/O error receiving the response.", e );
        } catch ( JSONException e ) {
            throw new ReCaptchaException( "Error parsing the response.", e );
        }
    }

    private boolean isValid( JSONObject responseJSON ) {
        return responseJSON.getBoolean( "success" );
    }

    @Nullable
    private String formatErrorCodes( JSONObject responseJSON ) {
        if ( !responseJSON.has( "error-codes" ) ) {
            return null;
        }
        return responseJSON.getJSONArray( "error-codes" )
                .toList()
                .stream()
                .map( Object::toString )
                .collect( Collectors.joining( ", " ) );
    }

    private String createUrlParameters( HttpServletRequest request ) {
        if ( request.getParameter( RESPONSE_REQUEST_PARAMETER ) == null ) {
            throw new ReCaptchaException( "Missing reCaptcha response parameter." );
        }
        return POST_PARAM_SECRET + "=" + urlEncode( privateKey )
                + "&" + POST_PARAM_RESPONSE + "=" + urlEncode( request.getParameter( RESPONSE_REQUEST_PARAMETER ) )
                + "&" + POST_PARAM_IP + "=" + urlEncode( request.getRemoteAddr() );
    }
}
