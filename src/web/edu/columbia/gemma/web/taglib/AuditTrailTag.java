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
package edu.columbia.gemma.web.taglib;

import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import edu.columbia.gemma.common.auditAndSecurity.AuditEvent;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;

/**
 * TODO - DOCUMENT ME
 * 
 * @jsp.tag name="auditTrail" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailTag extends TagSupport {

    private AuditTrail auditTrail;

    /**
     * @jsp.attribute description="The audit trail to be formatted" required="true" rtexprvalue="true"
     * @param auditTrail The auditTrail to set.
     */
    public void setAuditTrail( AuditTrail auditTrail ) {
        this.auditTrail = auditTrail;
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
        if ( this.auditTrail == null ) {
            buf.append( "No audit trail" );
        } else if ( this.auditTrail.getEvents().size() == 0 ) {
            buf.append( "Empty audit trail" );
        } else {
            buf.append( "<ol>" );
            for ( AuditEvent auditEvent : ( List<AuditEvent> ) auditTrail.getEvents() ) {
                buf.append( "<li>" );
                buf.append( auditEvent.getDate() + ": " + auditEvent.getAction() /* + " by "
                       + auditEvent.getPerformer().getUserName() */ + ( auditEvent.getNote() == null ? " " : " Note: " )
                        + auditEvent.getNote() );
                buf.append( "</li>" );
            }
            buf.append( "</ol>" );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "BibliographicReferenceTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
