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
package ubic.gemma.web.controller.common.description.bibref;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse; 
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.testing.BaseSpringWebTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedQueryControllerTest extends BaseSpringWebTest {
    private PubMedQueryController controller;

    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        controller = ( PubMedQueryController ) getBean( "pubMedQueryController" );
    }

    /**
     * Test method for
     * {@link ubic.gemma.web.controller.common.description.bibref.PubMedQueryController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)}.
     */
    public final void testOnSubmit() throws Exception {
        MockHttpServletRequest request = newPost( "/pubMedSearch.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addParameter( "accession", "134444" );
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "accession" );
        assertNull( "Errors in model: " + errors, errors );

        // verify that success messages are in the request
        assertNotNull( mv.getModel().get( "bibliographicReference" ) );
        assertNotNull( request.getSession().getAttribute( "messages" ) );
        assertEquals( "bibRefView", mv.getViewName() );

    }

    public final void testOnSubmitAlreadyExists() throws Exception {
        // put it in the system.
        this.getTestPersistentBibliographicReference( "12299" );

        MockHttpServletRequest request = newPost( "/pubMedSearch.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addParameter( "accession", "12299" );
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "accession" );
        assertNull( "Errors in model: " + errors, errors );
        // verify that success messages are in the request
        assertNotNull( mv.getModel().get( "bibliographicReference" ) );
        assertNotNull( request.getSession().getAttribute( "messages" ) );
        assertEquals( "bibRefView", mv.getViewName() );
    }

    public final void testOnSubmitInvalidValue() throws Exception {
        MockHttpServletRequest request = newPost( "/pubMedSearch.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addParameter( "accession", "bad idea" );
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "searchCriteria" );
        assertTrue( "Expected an error", errors != null );
        assertEquals( "bibRefSearch", mv.getViewName() );
    }

    public final void testOnSubmitNotFound() throws Exception {
        MockHttpServletRequest request = newPost( "/pubMedSearch.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addParameter( "accession", "13133314444" );
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "searchCriteria" );
        assertTrue( "Expected an error", errors != null );
        assertEquals( "bibRefSearch", mv.getViewName() );
    }

    public void testDisplayForm() throws Exception {
        MockHttpServletRequest request = newGet( "/pubMedSearch.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "bibRefSearch", mv.getViewName() );
    }
}
