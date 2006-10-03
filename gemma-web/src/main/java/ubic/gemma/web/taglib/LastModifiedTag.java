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
package ubic.gemma.web.taglib;

import java.io.File;
import java.util.Date;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.time.DateFormatUtils;

/**
 * @jsp.tag name="lastModified" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class LastModifiedTag extends TagSupport {

    /**
     * 
     */
    private static final long serialVersionUID = -8022433255595840885L;
    
    private String refFile;

    /**
     * @param name The reference file to use for the date.
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setRefFile( String refFile ) {
        this.refFile = refFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        StringBuilder buf = new StringBuilder();

        buf.append( "<div> Last Modified :  " );
        String ref = "";
        ref = pageContext.getServletContext().getRealPath( refFile );
        File mainFile = new File( ref );

        Date d = new Date( mainFile.lastModified() );
        String dateString = DateFormatUtils.format( d, "yyyy.MM.dd hh:mm" );
        buf.append( dateString );
        buf.append( "</div>" );

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "lastModifiedTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
