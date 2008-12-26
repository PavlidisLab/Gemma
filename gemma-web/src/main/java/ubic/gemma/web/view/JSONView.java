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
package ubic.gemma.web.view;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.web.servlet.View;

/**
 * @author paul
 * @version $Id$
 */
public class JSONView implements View {

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.View#getContentType()
     */
    public String getContentType() {
        return "text/plain";
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    public void render( Map map, HttpServletRequest reqest, HttpServletResponse response ) throws Exception {
        JSONObject jso = JSONObject.fromMap( map );
        response.setContentType( "text/plain" ); // this should already have been set, just checking.
        Writer writer = response.getWriter();
        String string = jso.toString();
        response.setContentLength( string.getBytes().length );
        writer.write( string );
    }

}
