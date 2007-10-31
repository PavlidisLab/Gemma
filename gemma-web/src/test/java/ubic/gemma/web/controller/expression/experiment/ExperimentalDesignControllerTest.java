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

import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author Kiran Keshav
 * @version $Id$
 */
public class ExperimentalDesignControllerTest extends BaseSpringContextTest {

    ExperimentalDesignService experimentalDesignService = null;

    @SuppressWarnings("unchecked")
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        experimentalDesignService = ( ExperimentalDesignService ) getBean( "experimentalDesignService" );
    }

    /**
     * Tests showing an experimentalDesign which is implemented in
     * {@link ubic.gemma.model.web.controller.expression.experiment.ExpressionExperimentController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    public void testShowExperimentalDesign() throws Exception {
        endTransaction();
        ExperimentalDesignController c = ( ExperimentalDesignController ) getBean( "experimentalDesignController" );

        MockHttpServletRequest req = new MockHttpServletRequest( "GET",
                "/experimentalDesign/showExperimentalDesign.html" );

        ExperimentalDesign ed = experimentalDesignService.findByName( "Experimental Design 0" );
        if ( ed == null ) {
            ed = ExperimentalDesign.Factory.newInstance();
            ed.setName( "Experimental Design 0" );
            ed = experimentalDesignService.create( ed );
        }

        assertTrue( ed != null );

        req.addParameter( "name", "Experimental Design 0" );

        req.addParameter( "id", String.valueOf( ed.getId() ) );

        req.setRequestURI( "/experimentalDesign/showExperimentalDesign.html" );

        ModelAndView mav = c.handleRequest( req, ( HttpServletResponse ) null );

        // Map m = mav.getModel();
        // assertNotNull( m.get( "expressionExperiments" ) );

        assertEquals( mav.getViewName(), "experimentalDesign.detail" );

        /* uncomment to persist and leave data in database */
        // setComplete();
    }
}
