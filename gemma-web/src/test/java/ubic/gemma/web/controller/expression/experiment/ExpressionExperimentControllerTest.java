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
package ubic.gemma.web.controller.expression.experiment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the ExpressionExperimentController.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentControllerTest extends BaseSpringContextTest {

    /**
     * Tests getting all the expressionExperiments, which is implemented in
     * {@link ubic.gemma.web.controller.expression.experiment.ExpressionExperimentController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetExpressionExperiments() throws Exception {
        log.debug( "-> (association), => (composition)" );

        /* persisterHelper.persist calls the ExpressionExperimentService, so you will not bypass security. */
        this.getTestPersistentExpressionExperiment();

        MockHttpServletRequest req = new MockHttpServletRequest( "GET",
                "/expressionExperiment/showAllExpressionExperiments.html" );
        req.setRequestURI( "/expressionExperiment/showAllExpressionExperiments.html" );

        // ModelAndView mav = expressionExperimentController.handleRequest( req, ( HttpServletResponse ) null );
        //
        // // ExpressionExperimentDao eeDao = ( ExpressionExperimentDao ) getBean( "expressionExperimentDao" );
        // // ExpressionExperiment expressionExperiment = eeDao.findByName( "Expression Experiment" );
        //
        // Map m = mav.getModel();
        //
        // assertNotNull( m.get( "expressionExperiments" ) );
        // assertEquals( mav.getViewName(), "expressionExperiments" );

        /* uncomment to leave data in the database */
        // this.
    }

    // /**
    // * @param expressionExperimentController the expressionExperimentController to set
    // */
    // public void setExpressionExperimentController( ExpressionExperimentController expressionExperimentController ) {
    // this.expressionExperimentController = expressionExperimentController;
    // }

    /**
     * @throws Exception
     */
    // @SuppressWarnings("unchecked")
    // public void testGetExperimentalDesigns() throws Exception {
    //
    // ExpressionExperimentController c = ( ExpressionExperimentController ) ctx
    // .getBean( "expressionExperimentController" );
    //
    // MockHttpServletRequest req = new MockHttpServletRequest( "GET", "Gemma/experimentalDesigns.htm" );
    // req.setRequestURI( "/Gemma/experimentalDesigns.htm" );
    // // cannot set parameter (setParmeter does not exist) so I had to set the attribute. On the server side,
    // // I have used a getAttribute as opposed to a getParameter - difference?
    // req.setAttribute( "name", "Expression Experiment" );
    //
    // ModelAndView mav = c.handleRequest( req, ( HttpServletResponse ) null );
    //
    // /*
    // * In this case, the map contains 1 element of type Collection. That is, a collection of experimental designs.
    // */
    // Map<String, Object> m = mav.getModel();
    //
    // Collection<ExperimentalDesign> col = ( Collection<ExperimentalDesign> ) m.get( "experimentalDesigns" );
    // log.debug( new Integer( col.size() ) );
    //
    // assertNotNull( m.get( "experimentalDesigns" ) );
    // assertEquals( mav.getViewName(), "experimentalDesign.GetAll.results.view" );
    // }
}
