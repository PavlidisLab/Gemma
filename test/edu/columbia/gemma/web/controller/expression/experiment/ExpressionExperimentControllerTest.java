/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * Tests the ExpressionExperimentController.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentControllerTest extends BaseControllerTestCase {
    private static Log log = LogFactory.getLog( ExpressionExperimentControllerTest.class.getName() );

    /**
     * Add a expressionExperiment to the database for testing purposes. Includes associations.
     */
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( "Expression Experiment" );
        /* Database entry is mandatory for expression experiments. */
        // FIXME - InvalidDataAccessApiUsageException - this is not a bi-directional relationship so
        // the solution on the twiki will not work. This is caused by something else.
        // DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        // ee.setAccession( de );
        /* Expression experiment contains a collection of experimental designs. */
        Collection<ExperimentalDesign> eeCol = new HashSet();
        int testNum = 3;
        for ( int i = 0; i < testNum; i++ ) {
            eeCol.add( ExperimentalDesign.Factory.newInstance() );
        }

        ee.setExperimentalDesigns( eeCol );

        /* Yes, we have access to the ctx in the setup. */
        ExpressionExperimentService ees = ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" );
        ees.createExpressionExperiment( ee );
    }

    /**
     * Tests getting all the expressionExperiments, which is implemented in
     * {@link edu.columbia.gemma.web.controller.expression.experiment.ExpressionExperimentController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    public void testGetExpressionExperiments() throws Exception {

        ExpressionExperimentController c = ( ExpressionExperimentController ) ctx
                .getBean( "expressionExperimentController" );

        MockHttpServletRequest req = new MockHttpServletRequest( "GET", "Gemma/expressionExperiments.htm" );

        ModelAndView mav = c.handleRequest( req, ( HttpServletResponse ) null );

        Map m = mav.getModel();

        assertNotNull( m.get( "expressionExperiments" ) );
        assertEquals( mav.getViewName(), "expressionExperiment.GetAll.results.view" );
    }
}
