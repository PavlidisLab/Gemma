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
package ubic.gemma.web.controller.testdata;

import java.util.Map;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse; 
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TestDataAddingControllerTest extends BaseSpringWebTest {
    TestDataAddingController controller;

    /**
     * @throws Exception
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        controller = ( TestDataAddingController ) getBean( "testDataAddingController" );

    }

    /**
     * Test method for
     * {@link ubic.gemma.web.controller.testdata.TestDataAddingController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)}.
     */
    public final void testOnSubmit() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/addTestData.html" );
        request.setRemoteUser( ConfigUtils.getString( "gemma.regular.user" ) );
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );

        assertTrue( "Errors returned in model: " + errors, errors == null );
        assertEquals( "Returned incorrect view name", "expressionExperiment.detail", mv.getViewName() );
        assertNotNull( mv );
        Map model = mv.getModel();
        assertTrue( model.containsKey( "expressionExperiment" ) );
        ExpressionExperiment ee = ( ExpressionExperiment ) model.get( "expressionExperiment" );
        assertTrue( "No persistent expresion experiment returned", ee != null && ee.getId() != null );
    }

    public final void testShowForm() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newGet( "/addTestData.html" );
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "returned incorrect view name", "testDataInsert", mv.getViewName() );
    }

}
