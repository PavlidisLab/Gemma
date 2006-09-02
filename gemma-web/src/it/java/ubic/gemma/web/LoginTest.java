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

import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class LoginTest extends BaseWebTest {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.BaseWebTest#setUp()
     */
    @Override
    public void setUp() throws Exception {
        // DO NOT use the superclass set up, becauses that logs us in.
        baseSetup();
    }

    public final void testLoginAndLogout() throws Exception {
        this.beginAt( "/mainMenu.html" );

        assertFormPresent();
        setTextField( "j_username", ConfigUtils.getString( "gemma.admin.user" ) );
        setTextField( "j_password", ConfigUtils.getString( "gemma.admin.password" ) );
        this.submit();

        assertTitleEquals( "Main Menu | Gemma" );
        //
        // assertTextPresent( "View array designs" );// FIXME - in messages_en.properties.

        // Logout
        clickLinkWithText( "Logout" );
        assertFormPresent();
    }

    public final void testSignupWithClientSideValidation() throws Exception {
        // assertJavascriptAlertPresent( "Username is a required field." ); // in 2.0 api
        // final WebClient webClient = new WebClient();
        // webClient.setAlertHandler( new CollectingAlertHandler() );

        this.beginAt( "/login.html" );
        assertFormPresent();
        // signup
        clickLinkWithText( "Signup" );
        assertFormPresent();

        // leave out user name on purpose, should get client-side validation error.
        setTextField( "password", "testing" );
        setTextField( "confirmPassword", "testing" );
        setTextField( "firstName", "testing" );
        setTextField( "lastName", "testing" );
        setTextField( "email", "testing@localhost.com" );
        setTextField( "passwordHint", "cat" );

        submit( "save" );

        assertFormPresent(); // should still be on page because of client-side validation
        assertSubmitButtonPresent();

        setTextField( "userName", "testing" );

        // can't do this because of alert?
        // submit();

        // this doesn't work at present
        // assertTextPresent( "You have successfully registered" );

        // edit profile

        // assertLinkPresentWithText( "Edit Profile" );
        // clickLinkWithText( "Edit Profile" );

        // sign is as admin

        // delete the user we just created

    }

    public final void testSignup() throws Exception {
        this.beginAt( "/login.html" );
        assertFormPresent();
        // signup
        clickLinkWithText( "Signup" );
        assertFormPresent();

        setTextField( "userName", "testing" );
        setTextField( "password", "testing" );
        setTextField( "confirmPassword", "testing" );
        setTextField( "firstName", "testing" );
        setTextField( "lastName", "testing" );
        setTextField( "email", "testing@localhost.com" );
        setTextField( "passwordHint", "cat" );

        submit( "save" );
        
        dumpHtml();
        this.gotoPage( "/mainMenu.html" );

        assertLinkNotPresentWithText( "Signup" );

    }

    // public final void testDeleteUser() throws Exception {
    //
    // }

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
