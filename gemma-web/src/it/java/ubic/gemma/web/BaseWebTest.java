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
package ubic.gemma.web;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.ConfigUtils;

import net.sourceforge.jwebunit.WebTestCase;

/**
 * Base functional test case that sets up the environment and logs you in. (don't override if you are testing login
 * functionality)
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BaseWebTest extends WebTestCase {
    private static Log log = LogFactory.getLog( BaseWebTest.class.getName() );

    /**
     * {@link http://www.it-eye.nl/weblog/2005/07/07/jwebunit-untrusted-certificates-https-and-proxies/}
     */
    private void setUpSSL() {
        // configure which keystore to use to validate certificate
        System.setProperty( "javax.net.ssl.trustStore", System.getProperty( "user.home" ) + File.separator
                + "dev.keystore" );
        System.setProperty( "javax.net.ssl.trustStorePassword", "toasted" );

        assertTrue( ( new File( System.getProperty( "javax.net.ssl.trustStore" ) ) ).canRead() );

        log.info( "Trusted test keystore is at " + System.getProperty( "javax.net.ssl.trustStore" ) + " with password "
                + System.getProperty( "javax.net.ssl.trustStorePassword" ) );
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        baseSetup();

        logIn();
    }

    /**
     * Perform the bare minimum of setup: do not log in.
     */
    protected void baseSetup() {
        getTestContext().setUserAgent( "Mozilla/5.0" );
        getTestContext().setBaseUrl( "http://localhost:8080/Gemma" );
        setUpSSL();
        log.debug( "Test base url=" + getTestContext().getBaseUrl() );
    }

    /**
     * 
     */
    private void logIn() {
        log.debug( "Logging in" );
        this.beginAt( "/login.jsp" );

        assertFormPresent();
        setTextField( "j_username", ConfigUtils.getString( "gemma.admin.user" ) );
        setTextField( "j_password", ConfigUtils.getString( "gemma.admin.password" ) );
        this.submit();
        assertTextPresent( "Main Menu" );

        log.info( "Logged in as " + ConfigUtils.getString( "gemma.admin.user" ) + ", ready for tests" );
    }
}
