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

import org.apache.commons.lang.RandomStringUtils;

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

    public final void testSignupWithValidationFailure() throws Exception {
        // assertJavascriptAlertPresent( "Username is a required field." ); // in 2.0 api
        // final WebClient webClient = new WebClient();
        // webClient.setAlertHandler( new CollectingAlertHandler() );

        this.beginAt( "/login.html" );
        assertFormPresent();
        // signup
        clickLinkWithText( "Signup" );
        assertFormPresent();
        String username = RandomStringUtils.randomAlphabetic( 10 );
        // leave out user name on purpose, should get client-side validation error.
        setTextField( "newPassword", "testing" );
        setTextField( "confirmNewPassword", "testing" );
        setTextField( "name", "testing" );
        setTextField( "lastName", "testing" );
        setTextField( "email", "testing@localhost.com" );
        setTextField( "passwordHint", "cat" );

        submit( "save" );

        assertFormPresent(); // should still be on page because of validation
        assertSubmitButtonPresent();

        setTextField( "userName", username );

        submit( "save" );

        assertTextPresent( "Main Menu" );

    }

    public final void testSignupAndLogin() throws Exception {
        this.beginAt( "/login.html" );
        assertFormPresent();

        // signup
        clickLinkWithText( "Signup" );
        assertFormPresent();
        String username = RandomStringUtils.randomAlphabetic( 10 );
        String password = "testing";
        setTextField( "userName", username );
        setTextField( "newPassword", password );
        setTextField( "confirmNewPassword", password );
        setTextField( "name", "testing" );
        setTextField( "lastName", "testing" );
        setTextField( "email", username + ".testing@localhost.com" );
        setTextField( "passwordHint", "cat" );

        submit( "save" );

        // should redirect to main menu, if javascript is on?

        setTextField( "j_username", username );
        setTextField( "j_password", password );

        submit();

        assertTextPresent( "Main Menu" );
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
