/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.web.controller.common;

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * Class to handle saving client-side javascript error messages and warnings to a server-side log.
 *
 * @author tvrossum
 *
 */
@Controller
public class JavascriptLogger {

    @Autowired
    private UserManager userManager = null;

    private static final Log log = LogFactory.getLog( "javascriptLogger" );

    @Value("${gemma.javascript.log}")
    private boolean needToLog;

    /**
     * Write to log with severity = "debug"
     *
     * @param userAgent details about user's browser, OS etc
     */
    public void writeToDebugLog( String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.debug( entry );
    }

    /**
     * Write to log with severity = "error"
     *
     * @param userAgent details about user's browser, OS etc
     */
    public void writeToErrorLog( String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.error( entry );
    }

    /**
     * Write to log with severity = "fatal"
     *
     * @param userAgent details about user's browser, OS etc
     */
    public void writeToFatalLog( String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.fatal( entry );
    }

    /**
     * Write to log with severity = "info"
     *
     * @param userAgent details about user's browser, OS etc
     */
    public void writeToInfoLog( String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.info( entry );
    }

    /**
     * Defaults to writing to log with severity = "info"
     *
     * @param userAgent details about user's browser, OS etc
     */
    public void writeToLog( String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.info( entry );
    }

    /**
     * Write to log with severity = "warn"
     *
     * @param userAgent details about user's browser, OS etc
     */
    public void writeToWarnLog( String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.warn( entry );
    }

    /**
     * Format input from front end into an (error) message for the log
     *
     * @param userAgent details about user's browser, OS etc
     * @return formatted string to write to log
     */
    private String formatLogEntry( String errorMessage, String url, String line, String href, String userAgent ) {
        // get user name or anon
        User user = null;
        String name = "anonymous";
        try {
            user = userManager.getCurrentUser();
        } catch ( org.springframework.security.core.userdetails.UsernameNotFoundException err ) {
            // used to happen when user is anon; now we just return null, so we should not reach this block. Just being
            // safe.
            name = "anonymous";
        }

        if ( user != null ) {
            name = user.getUserName() + " (id: " + user.getId() + ") (admin: " + SecurityUtil.isUserAdmin() + ")";
        }

        return "error[" + errorMessage + "] jsFile[" + url + ":ln" + line + "]" + " page[" + href + "] browser["
                + userAgent + "] user[" + name + "]";
    }
}
