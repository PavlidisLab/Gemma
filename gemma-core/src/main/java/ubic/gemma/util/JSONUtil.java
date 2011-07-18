/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.util;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * A utility class to do 'JSON-like' things, such as writing out JSON objects to the response {@link Writer}.
 * 
 * @author keshav
 * @version $Id$
 */
public class JSONUtil {

    private HttpServletResponse response = null;

    /**
     * @param request
     * @param response
     */
    public JSONUtil( HttpServletRequest request, HttpServletResponse response ) {
        super();
        this.response = response;
    }

    /**
     * Writes the text, which is in JSON format, to the {@link HttpServletResponse}.
     * 
     * @param jsonText
     */
    public void writeToResponse( String jsonText ) throws IOException {
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper( response );
        responseWrapper.setContentType( "text/plain" );
        responseWrapper.setContentLength( jsonText.getBytes().length );
        Writer out = responseWrapper.getWriter();
        out.write( jsonText );
        out.close();
    }

    public String getJSONErrorMessage( Exception e ) {       
        
        String errMsg;
        
        if (e.getCause()!=null){
            errMsg = e.getCause().getMessage();
        }
        else{
            errMsg = e.getLocalizedMessage();
        }
        String jsonText = "{success:false, message:\"" + errMsg + "\"}";
        return jsonText;
    }

}
