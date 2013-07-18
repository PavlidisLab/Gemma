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
package ubic.gemma.web.taglib.common.auditAndSecurity;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExceptionTag extends TagSupport {

    protected static Log log = LogFactory.getLog( ExceptionTag.class.getName() );

    /**
     * 
     */
    private static final long serialVersionUID = 4323477499674966726L;

    Exception exception;

    Boolean showStackTrace = true;

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
        try {
            final StringBuilder buf = new StringBuilder();
            if ( this.exception == null ) {
                buf.append( "Error was not recovered" );
            } else {

                if ( showStackTrace ) {
                    buf.append( "<div id=\"stacktrace\" class=\"stacktrace\" >" );
                    if ( exception.getStackTrace() != null ) {
                        buf.append( ExceptionUtils.getFullStackTrace( exception ) );
                    } else {
                        buf.append( "There was no stack trace!" );
                    }
                    buf.append( "</div>" );
                }
                // To make sure error doesn't get swallowed.
                log.error( this.exception, this.exception );
            }

            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "Exception tag threw an exception: " + ex.getMessage(), ex );
        }
        return SKIP_BODY;
    }

    /**
     * @param exception
     */
    public void setException( Exception exception ) {
        this.exception = exception;
    }

    /**
     * @param showStackTrace the showStackTrace to set
     */
    public void setShowStackTrace( Boolean showStackTrace ) {
        this.showStackTrace = showStackTrace;
    }

}
