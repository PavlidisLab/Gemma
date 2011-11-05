/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

/**
 * Simply prints text to the client. The model must have a parameter matching TEXT_PARAM which holds the text to be
 * written.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TextView extends AbstractView {

    /**
     * Name of parameter used to retrieve the text from the model.
     */
    public static final String TEXT_PARAM = "text";
 
    @Override
    protected void renderMergedOutputModel( Map model, HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        String textToRender = ( String ) model.get( TEXT_PARAM );
        response.setContentType( "text/plain" );
        response.setContentLength( textToRender.getBytes().length );
        response.getOutputStream().print( textToRender );
        response.getOutputStream().flush();
    }

}
