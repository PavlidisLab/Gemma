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
import ubic.gemma.web.controller.common.auditAndSecurity.SignupControllerTest;
import ubic.gemma.web.controller.common.description.BibRefControllerTest;
import ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignControllerTest;
import ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignFormControllerTest;
import ubic.gemma.web.controller.expression.bioAssay.BioAssayFormControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExperimentalDesignControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentControllerTest;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSearchControllerIntegrationTest;
import ubic.gemma.web.util.progress.ProgressDataTest;
import ubic.gemma.web.util.progress.ProgressIntegrationTest;

/**
 * Tests for gemma-web.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllWebTests extends TestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for gemma-web" );
        suite.addTestSuite( SignupControllerTest.class );
        suite.addTestSuite( BibRefControllerTest.class );

        suite.addTestSuite( ArrayDesignControllerTest.class );
        suite.addTestSuite( ArrayDesignFormControllerTest.class );

        suite.addTestSuite( BioAssayFormControllerTest.class );
        suite.addTestSuite( ExperimentalDesignControllerTest.class );
        suite.addTestSuite( ExpressionExperimentControllerTest.class );
        suite.addTestSuite( ExpressionExperimentSearchControllerIntegrationTest.class );
        suite.addTestSuite( ProgressDataTest.class );
        suite.addTestSuite( ProgressIntegrationTest.class );
        return suite;
    }
}
