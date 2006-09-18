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

/**
 * Test editing users and profiles.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class EditProfileTest extends BaseWebTest {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.BaseWebTest#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testEditProfileCancel() throws Exception {
        this.gotoPage( "/editProfile.html" );
        assertFormPresent();
        assertSubmitButtonPresent( "cancel" );
        this.submit( "cancel" );
        assertTextPresent( "cancelled" );
        assertLinkPresentWithText( "array design" );
    }

    public void testEditProfile() throws Exception {
        this.gotoPage( "/editUser.html?userName=testing&from=list" );
        assertFormPresent();
        dumpHtml();
        assertSubmitButtonPresent( "save" );
        this.setTextField( "passwordHint", "guess" );
        submit( "save" );

        assertTextPresent( "updated" );
        assertLinkPresentWithText( "active" );
    }

}
