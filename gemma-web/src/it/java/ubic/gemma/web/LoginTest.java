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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.ConfigUtils;

import net.sourceforge.jwebunit.WebTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author pavlidis
 * @version $Id$
 */
public class LoginTest extends WebTestCase {

    private static Log log = LogFactory.getLog( LoginTest.class.getName() );

    public static Test suite() {
        TestSuite suite = new TestSuite( LoginTest.class );
        return new ContainerTestSetup( suite );
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getTestContext().setUserAgent( "Mozilla" );
        getTestContext().setBaseUrl( "http://localhost:8080/Gemma" );
        log.info( getTestContext().getBaseUrl() );
    }

    public final void testLogin() throws Exception {
        this.beginAt( "/login.jsp" );

        assertFormPresent();
        setTextField( "j_username", ConfigUtils.getString( "gemma.admin.user" ) );
        setTextField( "j_password", ConfigUtils.getString( "gemma.admin.password" ) );
        this.submit();
        assertTextPresent( "Main Menu" );
        assertLinkPresentWithText( "View array designs" );// FIXME - in messages_en.properties.

        // Logout
        clickLinkWithText( "Logout" );
        assertFormPresent();
    }

    public final void testSignupAndEditProfile() throws Exception {
        this.beginAt( "/login.jsp" );
        assertFormPresent();
        // signup
        clickLinkWithText( "Signup" );
        assertFormPresent();

        // first try without confirmpassword == password
        setTextField( "userName", "testing" );
        setTextField( "password", "testing" );
        setTextField( "confirmPassword", "testingwrong" );
        setTextField( "firstName", "testing" );
        setTextField( "lastName", "testing" );
        setTextField( "email", "testing@localhost.com" );
        setTextField( "passwordHint", "cat" );
        submit();
        assertTextPresent( "The Password field has to have the same value as the Confirm Password field." );
        setTextField( "confirmPassword", "testing" );
        submit();

        assertTextPresent( "You have successfully registered for access to this application" );

        // edit profile

        assertLinkPresentWithText( "Edit Profile" );
        clickLinkWithText( "Edit Profile" );

        // sign is as admin

        // delete the user we just created
        
    }

    public final void testDeleteUser() throws Exception {
       
        
     
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.jwebunit.WebTestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
