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
package ubic.gemma.web.controller.util.view;

import org.json.JSONObject;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.util.Map;

/**
 * @author paul
 */
public class JSONView implements View {
    private String docType = "text/html";

    public JSONView() {
        super();
    }

    /**
     * @param docType e.g., text/html to switch from the default 'text/plain'.
     */
    public JSONView( String docType ) {
        this();
        this.docType = docType;
    }

    @Override
    public String getContentType() {
        return this.docType;
    }

    @Override
    public void render( Map<String, ?> map, HttpServletRequest reqest, HttpServletResponse response ) throws Exception {

        JSONObject jso = new JSONObject( map );
        response.setContentType( this.docType );
        try ( Writer writer = response.getWriter() ) {

            // Need to wrap json in html tags or the proxy server will wrap in <p></p> tags.
            // will work in test enviroment (with or without wrapping json)
            // only problematic on production (no proxy in test enviroment)
            // This is specifically for extjs and uploading a file.
            // Other frameworks might not like this.

            writer.write( "<html><body>" + jso + "</body></html>" );
        }
    }

}
