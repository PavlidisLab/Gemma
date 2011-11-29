/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
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

package ubic.gemma.web.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.ConfigUtils;


/**
 * Class to handle saving client-side javascript error messages and warnings to a server-side log.
 * 
 * @author tvrossum
 * @version $Id$
 */
@Controller
public class JavascriptLogger {
    
    @Autowired
    private UserManager userManager = null;
    
    private static Log log = LogFactory.getLog( "javascriptLogger" );

    private static boolean needToLog = ConfigUtils.getBoolean( "gemma.javascript.log" );
    /**
     * TODO allow option to control writing to info vs warn TODO include session info (want to be able to follow what
     * someone did?)
     * 
     * @param message
     */
    public void writeToLog(String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.info( entry );
    }
    
    public void writeToDebugLog(String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.debug( entry );
    }
    
    public void writeToInfoLog(String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.info( entry );
    }
    
    public void writeToWarnLog(String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.warn( entry );
    }
    
    public void writeToErrorLog(String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.error( entry );
    }
    
    public void writeToFatalLog(String errorMessage, String url, String line, String href, String userAgent ) {
        if ( !needToLog ) return;
        String entry = this.formatLogEntry( errorMessage, url, line, href, userAgent );
        log.fatal( entry );
    }

    /**
     * TODO allow option to control writing to info vs warn
     * 
     * @param messages
     */
    public void writeMultipleToLog( String[][] messages ) {
        if ( !needToLog ) return;
        for ( int i = 0; i < messages.length; i++ ) {
            log.info( messages[i] );
        }
    }
    
    private String formatLogEntry(String errorMessage, String url, String line, String href, String userAgent ){
        // get user name or anon
        User user = null;
        String name = "";
        try{
           user = userManager.getCurrentUser(); 
        }catch(org.springframework.security.core.userdetails.UsernameNotFoundException err){
            // happens when user is anon
            name = "anon";
        }
        
        if(user != null){
            name = user.getUserName()+" (id: "+user.getId()+") (admin: "+SecurityService.isUserAdmin()+")";
        }
        
        return "error[" + errorMessage + "] jsFile[" + url + ":ln" + line + "]" + 
            " page[" + href + "] browser[" + userAgent + "] user["+name+"]";
    }
}
