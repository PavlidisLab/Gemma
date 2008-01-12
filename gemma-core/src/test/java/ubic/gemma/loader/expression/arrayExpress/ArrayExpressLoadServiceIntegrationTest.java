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
package ubic.gemma.loader.expression.arrayExpress;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * These are full-sized tests.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressLoadServiceIntegrationTest extends BaseSpringContextTest {

    final public void testLoad() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
        ExpressionExperiment experiment = svc.load( "E-MEXP-955" );
        assertNotNull( experiment );
    }

    /**
     * This test fails as of 1/2008 due to problems with the input files.
     * 
     * @throws Exception
     */
    final public void testLoadWithAD() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
        ExpressionExperiment experiment = svc.load( "E-MEXP-955", "GPL81" );
        assertNotNull( experiment );
    }

    final public void testLoadWithNameMismatch() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
        ExpressionExperiment experiment = svc.load( "E-TABM-302", "GPL81" );
        assertNotNull( experiment );
    }

}
