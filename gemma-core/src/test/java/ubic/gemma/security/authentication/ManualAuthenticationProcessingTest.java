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
package ubic.gemma.security.authentication;

import org.springframework.security.BadCredentialsException;

import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ManualAuthenticationProcessingTest extends BaseSpringContextTest {

    public void testAttemptAuthentication() throws Exception {
        ManualAuthenticationProcessing manager = ( ManualAuthenticationProcessing ) this
                .getBean( "manualAuthenticationProcessing" );
        assertNotNull( manager );
        try {
            manager.attemptAuthentication( ConfigUtils.getString( "gemma.regular.user" ), ConfigUtils
                    .getString( "gemma.regular.password" ) );

        } catch ( BadCredentialsException expected ) {
            fail( "Should not have gotten a BadCredentialsException" );
        }

    }

    public void testAttemptAuthenticationWrongPassword() throws Exception {
        ManualAuthenticationProcessing manager = ( ManualAuthenticationProcessing ) this
                .getBean( "manualAuthenticationProcessing" );
        assertNotNull( manager );

        try {
            manager.attemptAuthentication( ConfigUtils.getString( "gemma.regular.user" ), "wrong" );
            fail( "Should have gotten a BadCredentialsException" );
        } catch ( BadCredentialsException expected ) {
            // expected.
        }

    }

    public void testAttemptAuthenticationNonexistentUser() throws Exception {
        ManualAuthenticationProcessing manager = ( ManualAuthenticationProcessing ) this
                .getBean( "manualAuthenticationProcessing" );
        assertNotNull( manager );
        try {
            manager.attemptAuthentication( "I don't exist", "wrong" );
            fail( "Should have gotten a BadCredentialsException" );
        } catch ( BadCredentialsException expected ) {
            // expected.
        }
    }

}
