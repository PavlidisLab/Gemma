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
package ubic.gemma;

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.web.controller.TaskRunningTest;
import ubic.gemma.web.controller.common.description.BibRefControllerTest;
import ubic.gemma.web.controller.common.description.bibref.PubMedQueryControllerTest;
import ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignControllerTest;
import ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignFormControllerTest;
import ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignSequenceAddControllerTest;
import ubic.gemma.web.controller.expression.bioAssay.BioAssayFormControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExperimentalDesignControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExperimentalDesignFormControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentVisualizationFormControllerIntegrationTest;
import ubic.gemma.web.controller.testdata.TestDataAddingControllerTest;
import ubic.gemma.web.listener.StartupListenerTest;
import ubic.gemma.web.util.ConfigurationCookieTest;
import ubic.gemma.web.validation.ValidationTest;

/**
 * Tests for gemma-web.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllWebTests extends TestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for gemma-web" );

        suite.addTestSuite( BibRefControllerTest.class );

        suite.addTestSuite( PubMedQueryControllerTest.class );

        suite.addTestSuite( ArrayDesignControllerTest.class );
        suite.addTestSuite( ArrayDesignFormControllerTest.class );
        suite.addTestSuite( ArrayDesignSequenceAddControllerTest.class );

        suite.addTestSuite( BioAssayFormControllerTest.class );

        suite.addTestSuite( ExperimentalDesignControllerTest.class );
        suite.addTestSuite( ExperimentalDesignFormControllerTest.class );
        suite.addTestSuite( ExpressionExperimentControllerTest.class );
        suite.addTestSuite( ExpressionExperimentVisualizationFormControllerIntegrationTest.class );
        suite.addTestSuite( ExpressionExperimentLoadControllerTest.class );

        suite.addTestSuite( TestDataAddingControllerTest.class );

        suite.addTestSuite( StartupListenerTest.class );
        suite.addTestSuite( ValidationTest.class );

        suite.addTestSuite( TaskRunningTest.class );

        suite.addTestSuite( ConfigurationCookieTest.class );

        System.out.print( "----------------------\nGemma Web Tests\n" + suite.countTestCases()
                + " Tests to run\n----------------------\n" );

        return suite;
    }
}