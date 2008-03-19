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
package ubic.gemma.security;

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.security.authentication.ManualAuthenticationProcessingTest;
import ubic.gemma.security.interceptor.AuditInterceptorTest;
import ubic.gemma.security.interceptor.PersistAclInterceptorTest;
import ubic.gemma.security.principal.PrincipalTest;

/**
 * Test for ubic.gemma.security
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllSecurityTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.security" );
        // $JUnit-BEGIN$
        suite.addTestSuite( SecurityIntegrationTest.class );
        suite.addTestSuite( ManualAuthenticationProcessingTest.class );
        suite.addTestSuite( PrincipalTest.class );
        suite.addTestSuite( AuditInterceptorTest.class );
        suite.addTestSuite( PersistAclInterceptorTest.class );
        suite.addTestSuite( UserGroupServiceTest.class );
        suite.addTestSuite( SecurityServiceTest.class );
        // $JUnit-END$
        return suite;
    }

}
