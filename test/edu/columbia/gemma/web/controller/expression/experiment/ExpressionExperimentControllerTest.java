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
package edu.columbia.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseTransactionalSpringContextTest;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * Tests the ExpressionExperimentController.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentControllerTest extends BaseTransactionalSpringContextTest {
    private static Log log = LogFactory.getLog( ExpressionExperimentControllerTest.class.getName() );

    /**
     * Add a expressionExperiment to the database for testing purposes. Includes associations.
     */
    @SuppressWarnings("unchecked")
    public void onSetUpInTranasaction() throws Exception {
        super.onSetUpInTransaction();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( "Expression Experiment" );
        /* Database entry is mandatory for expression experiments. */
        // FIXME - InvalidDataAccessApiUsageException - this is not a bi-directional relationship so
        // the solution on the twiki will not work. This is caused by something else.
        // DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        // ee.setAccession( de );
        /* Expression experiment contains a collection of experimental designs. */

        int testNum = 3;

        Collection<ExperimentalFactor> efCol = new HashSet();
        for ( int i = 0; i < testNum; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            OntologyEntry oe = OntologyEntry.Factory.newInstance();
            oe.setAccession( "oe:" + i );
            oe.setDescription( "Ontology Entry " + i );

            log.debug( "ontolgy entry  => experimental factor." );
            ef.setCategory( oe );
            // ef.setAnnotations(oeCol);
            // ef.setFactorValues(fvCol);

        }

        Collection<ExperimentalDesign> edCol = new HashSet();
        for ( int i = 0; i < testNum; i++ ) {
            ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
            ed.setName( "Experimental Design " + i );
            ed.setDescription( i + ": A test experimental design." );

            log.debug( "experimental factors => experimental design." );
            ed.setExperimentalFactors( efCol ); // set test experimental factors

            edCol.add( ed ); // add experimental designs
        }

        log.debug( "experimental designs => expression experiment" );
        ee.setExperimentalDesigns( edCol );

        Collection<BioAssay> baCol = new HashSet();
        for ( int i = 0; i < testNum; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "Bioassay " + i );
            ba.setDescription( i + ": A test bioassay." );
            baCol.add( ba );
        }

        log.debug( "bioassays => expression experiment." );
        ee.setBioAssays( baCol );

        ExpressionExperimentService ees = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        log.debug( "Loading test expression experiment." );
        if ( ees.findByName( ee.getName() ) == null ) ees.findOrCreate( ee );
    }

    /**
     * Tests getting all the expressionExperiments, which is implemented in
     * {@link edu.columbia.gemma.web.controller.expression.experiment.ExpressionExperimentController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    public void testGetExpressionExperiments() throws Exception {

        ExpressionExperimentController c = ( ExpressionExperimentController ) getBean( "expressionExperimentController" );

        MockHttpServletRequest req = new MockHttpServletRequest( "GET",
                "/expressionExperiment/showAllExpressionExperiments.html" );
        req.setRequestURI( "/expressionExperiment/showAllExpressionExperiments.html" );

        ModelAndView mav = c.handleRequest( req, ( HttpServletResponse ) null );

        Map m = mav.getModel();

        assertNotNull( m.get( "expressionExperiments" ) );
        assertEquals( mav.getViewName(), "expressionExperiments" );
    }

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
