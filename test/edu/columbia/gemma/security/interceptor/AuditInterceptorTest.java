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
package edu.columbia.gemma.security.interceptor;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AuditInterceptorTest extends BaseDAOTestCase {

    public void testSimpleAuditAddition() throws Exception {
        ExpressionExperimentService ees = ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );
        ee = ees.findOrCreate( ee );
        ees.delete( ee );
    }

    // FIXME add tests on collections and of update, create, remove...

}
