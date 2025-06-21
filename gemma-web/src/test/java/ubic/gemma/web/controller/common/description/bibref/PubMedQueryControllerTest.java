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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.support.SimpleSessionStatus;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class PubMedQueryControllerTest extends BaseSpringWebTest {

    @Autowired
    private PubMedQueryController controller;

    @Test
    public void testDisplayForm() {
        assertEquals( "bibRefSearch", controller.getView() );
    }

    @Test
    public final void testOnSubmit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest( "POST", "/pubMedSearch.html" );
        request.addParameter( "accession", "134444" );

        try {
            ModelAndView mv = controller.onSubmit( request, new PubMedSearchCommand( "134444" ),
                    new BeanPropertyBindingResult( new PubMedSearchCommand( "134444" ), "searchCriteria" ),
                    new SimpleSessionStatus() );
            Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "accession" );
            assertNull( "Errors in model: " + errors, errors );

            // verify that success messages are in the request
            assertNotNull( mv.getModel().get( "bibliographicReference" ) );
            // assertNotNull( request.getSession().getAttribute( "messages" ) );
            assertEquals( "bibRefView", mv.getViewName() );
        } catch ( Exception e ) {
            if ( e.getCause() instanceof IOException && e.getCause().getMessage().contains( "502" ) ) {
                log.warn( "Error 502 from NCBI, skipping test" );
                return;
            } else if ( e.getCause() instanceof IOException && e.getCause().getMessage().contains( "503" ) ) {
                log.warn( "Error 503 from NCBI, skipping test" );
                return;
            }
            throw ( e );
        }

    }

    @Test
    public final void testOnSubmitAlreadyExists() throws Exception {
        // put it in the system.
        this.getTestPersistentBibliographicReference( "12299" );

        MockHttpServletRequest request = new MockHttpServletRequest( "POST", "/pubMedSearch.html" );

        ModelAndView mv = controller.onSubmit( request, new PubMedSearchCommand( "12299" ),
                new BeanPropertyBindingResult( new PubMedSearchCommand( "12299" ), "searchCriteria" ),
                new SimpleSessionStatus() );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "searchCriteria" );
        assertNull( "Errors in model: " + errors, errors );
        assertNotNull( mv.getModel().get( "bibliographicReference" ) );
        // assertNotNull( request.getSession().getAttribute( "messages" ) );
        assertEquals( "bibRefView", mv.getViewName() );
    }

    @Test
    public final void testOnSubmitInvalidValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest( "POST", "/pubMedSearch.html" );
        ModelAndView mv = controller.onSubmit( request, new PubMedSearchCommand( "bad idea" ),
                new BeanPropertyBindingResult( new PubMedSearchCommand( "bad idea" ), "searchCriteria" ),
                new SimpleSessionStatus() );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "searchCriteria" );
        assertTrue( "Expected an error", errors != null );
        assertEquals( "bibRefSearch", mv.getViewName() );
    }

    @Test
    public final void testOnSubmitNotFound() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest( "POST", "/pubMedSearch.html" );
        ModelAndView mv = controller.onSubmit( request, new PubMedSearchCommand( "13133333314444" ),
                new BeanPropertyBindingResult( new PubMedSearchCommand( "13133333314444" ), "searchCriteria" ),
                new SimpleSessionStatus() );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "searchCriteria" );
        assertTrue( "Expected an error", errors != null );
        assertEquals( "bibRefSearch", mv.getViewName() );
    }
}
