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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.Constants;
import ubic.gemma.util.SslUtil;

/**
 * This tag library is designed to be used on a JSP to switch HTTP -> HTTPS protocols and vise versa. If you want to
 * force the page to be viewed in SSL, then you would do something like this:<br />
 * <br />
 * 
 * <pre>
 *       
 *        
 *         &lt;tag:secure /&gt;
 *         or
 *         &lt;tag:secure mode=&quot;secured&quot; /&gt;
 *         
 *        
 * </pre>
 * 
 * If you want the force the page to be viewed in over standard http, then you would do something like:<br />
 * 
 * <pre>
 *       
 *        
 *         &lt;tag:secure mode=&quot;unsecured&quot; /&gt;
 *         
 *        
 * </pre>
 * 
 * @jsp.tag name="secure" body-content="empty"
 * @author <a href="mailto:jon.lipsky@xesoft.com">Jon Lipsky</a> Contributed by: XEsoft GmbH Oskar-Messter-Strasse 18
 *         85737 Ismaning, Germany http://www.xesoft.com
 * @version $Id$
 */
public class SecureTag extends BodyTagSupport {
    // ~ Static fields/initializers =============================================

    public static final String MODE_SECURED = "secured";
    public static final String MODE_UNSECURED = "unsecured";
    public static final String MODE_EITHER = "either";

    // ~ Instance fields ========================================================

    private final Log log = LogFactory.getLog( SecureTag.class );
    protected String TAG_NAME = "Secure";
    private String mode = MODE_SECURED;
    private String httpPort = null;
    private String httpsPort = null;

    // ~ Methods ================================================================

    /**
     * Sets the mode attribute. This is included in the tld file.
     * 
     * @jsp.attribute description="The mode attribute (secure | unsecured)" required="false" rtexprvalue="true"
     */
    public void setMode( String aMode ) {
        mode = aMode;
    }

    @Override
    public int doStartTag() {
        // get the port numbers from the application context
        Map config = ( HashMap ) pageContext.getServletContext().getAttribute( Constants.CONFIG );

        httpPort = ( String ) config.get( Constants.HTTP_PORT );

        if ( httpPort == null ) {
            httpPort = SslUtil.STD_HTTP_PORT;
        }

        httpsPort = ( String ) config.get( Constants.HTTPS_PORT );

        if ( httpsPort == null ) {
            httpsPort = SslUtil.STD_HTTPS_PORT;
        }

        return SKIP_BODY;
    }

    @Override
    public int doAfterBody() {
        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        if ( mode.equalsIgnoreCase( MODE_SECURED ) ) {
            if ( pageContext.getRequest().isSecure() == false ) {
                String vQueryString = ( ( HttpServletRequest ) pageContext.getRequest() ).getQueryString();
                String vPageUrl = ( ( HttpServletRequest ) pageContext.getRequest() ).getRequestURI();
                String vServer = pageContext.getRequest().getServerName();

                StringBuffer vRedirect = new StringBuffer( "" );
                vRedirect.append( "https://" );
                vRedirect.append( vServer + ":" + httpsPort + vPageUrl );

                if ( vQueryString != null ) {
                    vRedirect.append( "?" );
                    vRedirect.append( vQueryString );
                }

                if ( log.isDebugEnabled() ) {
                    log.debug( "attempting to redirect to: " + vRedirect );
                }

                try {
                    ( ( HttpServletResponse ) pageContext.getResponse() ).sendRedirect( vRedirect.toString() );

                    return SKIP_PAGE;
                } catch ( Exception exc2 ) {
                    throw new JspException( exc2.getMessage() );
                }
            }
        } else if ( mode.equalsIgnoreCase( MODE_UNSECURED ) ) {
            if ( pageContext.getRequest().isSecure() == true ) {
                String vQueryString = ( ( HttpServletRequest ) pageContext.getRequest() ).getQueryString();
                String vPageUrl = ( ( HttpServletRequest ) pageContext.getRequest() ).getRequestURI();
                String vServer = pageContext.getRequest().getServerName();

                StringBuffer vRedirect = new StringBuffer( "" );
                vRedirect.append( "http://" );
                vRedirect.append( vServer + vPageUrl );

                if ( vQueryString != null ) {
                    vRedirect.append( "?" );
                    vRedirect.append( vQueryString );
                }

                try {
                    ( ( HttpServletResponse ) pageContext.getResponse() ).sendRedirect( vRedirect.toString() );

                    return SKIP_PAGE;
                } catch ( Exception exc2 ) {
                    throw new JspException( exc2.getMessage() );
                }
            }
        } else if ( mode.equalsIgnoreCase( MODE_EITHER ) ) {
            return EVAL_PAGE;
        } else {
            throw new JspException( "Illegal value for the attribute mode: " + mode );
        }

        return EVAL_PAGE;
    }
}
