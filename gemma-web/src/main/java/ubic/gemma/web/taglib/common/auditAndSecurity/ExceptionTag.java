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

import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import ubic.gemma.core.context.EnvironmentProfiles;

import javax.servlet.jsp.tagext.Tag;

import static org.springframework.web.util.HtmlUtils.htmlEscape;


/**
 * @author pavlidis
 */
@Setter
public class ExceptionTag extends RequestContextAwareTag {

    private static final long serialVersionUID = 4323477499674966726L;
    private static final Log log = LogFactory.getLog( ExceptionTag.class.getName() );

    /**
     * Exception to display.
     */
    private Exception exception;

    /**
     * Display the stacktrace.
     * <p>
     * Note: the stack trace is never display to non-administrators.
     */
    private boolean showStackTrace = true;

    @Override
    public int doStartTagInternal() {
        try {
            final StringBuilder buf = new StringBuilder();
            buf.append( "<div class=\"exception\">" );
            if ( this.exception == null ) {
                buf.append( "Error was not recovered" );
            } else {
                buf.append( "<p class=\"message\">" ).append( htmlEscape( exception.getMessage() ) ).append( "</p>" );
                if ( showStackTrace && ( isDev() || isAdmin() ) ) {
                    buf.append( "<div class=\"stacktrace mb-3\">" );
                    buf.append( htmlEscape( ExceptionUtils.getStackTrace( exception ) ) );
                    buf.append( "</div>" );
                    if ( exception.getCause() != null ) {
                        Throwable rootCause = ExceptionUtils.getRootCause( exception );
                        buf.append( "<h2>Root cause</h2>" );
                        buf.append( "<p class=\"message\">" ).append( htmlEscape( rootCause.getMessage() ) ).append( "</p>" );
                        buf.append( "<div class=\"stacktrace mb-3\">" );
                        buf.append( htmlEscape( ExceptionUtils.getStackTrace( rootCause ) ) );
                        buf.append( "</div>" );
                    }
                    if ( isDev() && !isAdmin() ) {
                        buf.append( "<p><b>Note:</b> You are in development mode, so you can see the stack trace. Otherwise only administrators can see this.</p>" );
                    }
                }
            }
            buf.append( "</div>" );
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

    /**
     * Check if the development profile is active.
     */
    private boolean isDev() {
        return getRequestContext().getWebApplicationContext().getEnvironment()
                .acceptsProfiles( EnvironmentProfiles.DEV );
    }

    /**
     * Check if the current user is an administrator.
     */
    private boolean isAdmin() {
        // this bean is declared twice: once for method security in gemma-core and a second time for Web security
        AccessDecisionManager accessDecisionManager = getRequestContext().getWebApplicationContext()
                .getBean( "httpAccessDecisionManager", AccessDecisionManager.class );
        try {
            accessDecisionManager.decide( SecurityContextHolder.getContext().getAuthentication(), null,
                    SecurityConfig.createList( "GROUP_ADMIN" ) );
            return true;
        } catch ( AccessDeniedException e ) {
            return false;
        }
    }
}
