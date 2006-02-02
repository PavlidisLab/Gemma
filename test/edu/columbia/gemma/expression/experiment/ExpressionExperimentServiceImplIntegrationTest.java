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
package edu.columbia.gemma.expression.experiment;

import edu.columbia.gemma.BaseServiceTestCase;

/**
 * Use this to test the acegi functionality with a cascade=all association when creating objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentServiceImplIntegrationTest extends BaseServiceTestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testCreateExpressionExperimentCascade() {
        ExpressionExperimentService ees = ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( "foo" );
        ee.getExperimentalDesigns().add( ed );

        ees.findOrCreate( ee );
    }

}