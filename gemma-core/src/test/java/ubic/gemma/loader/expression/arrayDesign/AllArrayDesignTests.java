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
package ubic.gemma.loader.expression.arrayDesign;

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.analysis.service.CompositeSequenceGeneMapperServiceIntegrationTest;

/**
 * Test for ubic.gemma.loader.expression.arrayDesign
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllArrayDesignTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for ubic.gemma.loader.expression.arrayDesign" );
        // $JUnit-BEGIN$
          suite.addTestSuite( ArrayDesignParserIntegrationTest.class );
        suite.addTestSuite( IlluminaProbeReaderTest.class );
        suite.addTestSuite( AffyProbeReaderTest.class );
        suite.addTestSuite( ArrayDesignSequenceProcessorTest.class );
        suite.addTestSuite( CompositeSequenceParserTest.class );
        suite.addTestSuite( CompositeSequenceGeneMapperServiceIntegrationTest.class );
        suite.addTestSuite( ArrayDesignProbeMapperServiceIntegrationTest.class );
        suite.addTestSuite( ArrayDesignSequenceAlignmentServiceIntegrationTest.class );
        suite.addTestSuite( ArrayDesignSequenceProcessorFastacmdTest.class );
        // $JUnit-END$
        return suite;
    }

}
