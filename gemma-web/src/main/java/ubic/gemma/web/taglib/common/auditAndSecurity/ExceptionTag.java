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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;


/**
 * @author pavlidis
 */
@SuppressWarnings("unused") // Frontend use
public class ExceptionTag extends TagSupport {

    private static final long serialVersionUID = 4323477499674966726L;
    private static final Log log = LogFactory.getLog( ExceptionTag.class.getName() );

    private Exception exception;
    private Boolean showStackTrace = true;

    @Override
    public int doStartTag() {
        try {
            final StringBuilder buf = new StringBuilder();
            if ( this.exception == null ) {
                buf.append( "Error was not recovered" );
            } else {
                if ( showStackTrace ) {
                    buf.append( "<div id=\"stacktrace\" class=\"stacktrace\" >" );
                    if ( exception.getStackTrace() != null ) {
                        buf.append( escapeHtml4( ExceptionUtils.getStackTrace( exception ) ) );
                    } else {
                        buf.append( "There was no stack trace!" );
                    }
                    buf.append( "</div>" );
                }
            }
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            /*
             * Avoid stack overflow...
             */
            ExceptionTag.log.error( "Exception tag threw an exception: " + ExceptionUtils.getRootCauseMessage( ex ), ex );
            if ( this.exception != null ) {
                ExceptionTag.log.error( "The original exception was: " + ExceptionUtils.getRootCauseMessage( ex ), exception );
            }
        }
        return Tag.SKIP_BODY;
    }

    @Override
    public int doEndTag() {
        return Tag.EVAL_PAGE;
    }

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
