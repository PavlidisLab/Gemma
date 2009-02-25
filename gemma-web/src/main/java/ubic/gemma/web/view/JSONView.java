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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;

/**
 * @author paul
 * @version $Id$
 */
public class JSONView implements View {

    Log log = LogFactory.getLog( this.getClass() );

    private String docType = "text/plain";

    /**
     * @param docType e.g., text/html to switch from the default 'text/plain'.
     */
    public JSONView( String docType ) {
        this();
        this.docType = docType;
    }

    public JSONView() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.View#getContentType()
     */
    public String getContentType() {
        return this.docType;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    public void render( Map map, HttpServletRequest reqest, HttpServletResponse response ) throws Exception {
        JSONObject jso = JSONObject.fromObject( map );
        response.setContentType( this.docType );
        log.debug( jso.toString() );
        Writer writer = response.getWriter();
        writer.write( "<html><body>"  + jso.toString() + "</body></html>" );
        writer.close();
        //jso.write( writer );
    }

}
