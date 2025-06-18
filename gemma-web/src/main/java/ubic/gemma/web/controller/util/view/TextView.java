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
package ubic.gemma.web.controller.util.view;

import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractView;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Simply prints text to the client. The model must have a parameter matching TEXT_PARAM which holds the text to be
 * written.
 *
 * @author pavlidis
 *
 */
public class TextView extends AbstractView {

    /**
     * Name of parameter used to retrieve the text from the model.
     */
    public static final String TEXT_PARAM = "text";

    private final String contentType;

    @Nullable
    private String contentDisposition;

    /**
     * @param textMediaSubType the subtype of {@code text/*} media type to use.
     */
    public TextView( String textMediaSubType ) {
        this.contentType = "text/" + textMediaSubType;
    }

    /**
     * Create a text view for {@code text/plain} content.
     */
    public TextView() {
        this( "plain" );
    }

    public void setContentDisposition( @Nullable String contentDisposition ) {
        this.contentDisposition = contentDisposition;
    }

    @Override
    protected void renderMergedOutputModel( Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response ) throws Exception {
        Assert.isTrue( model.get( TEXT_PARAM ) instanceof String, "The model must contain a string entry for " + TEXT_PARAM + "." );
        String textToRender = ( String ) model.get( TEXT_PARAM );
        response.setContentType( contentType );
        response.setContentLength( textToRender.getBytes().length );
        if ( contentDisposition != null ) {
            response.setHeader( "Content-Disposition", contentDisposition );
        }
        response.getWriter().print( textToRender );
        response.getWriter().flush();
    }

}
