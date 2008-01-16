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
package ubic.gemma.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.util.progress.ProgressDataTest;

/**
 * Test for gemma-util
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllUtilTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.util" );
        // $JUnit-BEGIN$
        suite.addTestSuite( BeanPropertyCompleterTest.class );
        suite.addTestSuite( CommonsConfigurationPropertyPlaceholderConfigurerTest.class );
        suite.addTestSuite( ConfigUtilsTest.class );
        suite.addTestSuite( ProgressDataTest.class );
        suite.addTestSuite( DateUtilTest.class );
        suite.addTestSuite( CountingMapTest.class );
        // $JUnit-END$
        return suite;
    }

}
