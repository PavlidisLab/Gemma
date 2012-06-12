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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.auditAndSecurity.Contact;

/**
  * @author Kiran Keshav
 * @version $Id$
 */
public class ContactTag extends TagSupport {
    /**
     * 
     */
    private static final long serialVersionUID = -6477171433937178582L;

    private static Log log = LogFactory.getLog( ContactTag.class );

    private Contact contact;

    /**
     * @param contact
     */
    public void setContact( Contact contact ) {

        log.debug( "set contact: " + contact );

        this.contact = contact;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        log.debug( "end tag" );

        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

        StringBuilder buf = new StringBuilder();
        if ( this.contact == null ) {
            buf.append( "No owner" );
        } else {
            buf.append( "<ol>" );
            buf.append( "<li>" );
            buf.append( contact.getName() );
            buf.append( contact.getAddress() );
            buf.append( contact.getPhone() );
            buf.append( contact.getFax() );
            buf.append( contact.getEmail() );
            buf.append( "</li>" );

            buf.append( "</ol>" );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ContactTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

}
