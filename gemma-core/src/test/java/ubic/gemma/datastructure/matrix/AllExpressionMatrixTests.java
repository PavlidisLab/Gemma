/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.datastructure.matrix;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AllExpressionMatrixTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.datastructure.matrix" );
        // $JUnit-BEGIN$
        suite.addTestSuite( ExpressionDataDoubleMatrixTest.class );
        suite.addTestSuite( MatrixConversionTest.class ); 
        // $JUnit-END$
        return suite;
    }

}
