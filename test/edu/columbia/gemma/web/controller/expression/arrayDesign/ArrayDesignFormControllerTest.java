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
package edu.columbia.gemma.web.controller.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseTransactionalSpringContextTest;
import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignFormControllerTest extends BaseTransactionalSpringContextTest {
    private static Log log = LogFactory.getLog( ArrayDesignFormControllerTest.class.getName() );

    ArrayDesign ad = null;

    private MockHttpServletRequest request = null;

    /**
     * setUp
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        ad = ArrayDesign.Factory.newInstance();
        ad.setName( "AD Bot" );
        ad.setDescription( "An array design created in the ArrayDesignFormControllerTest." );

        Contact c = Contact.Factory.newInstance();
        c.setName( "\'Contact Name\'" );
        ad.setDesignProvider( c );

        /* Database entry is mandatory for expression experiments. */
        // FIXME - InvalidDataAccessApiUsageException - this is not a bi-directional relationship so
        // the solution on the twiki will not work. This is caused by something else.
        // DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        // ee.setAccession( de );
        /* Expression experiment contains a collection of experimental designs. */

        // ***********
        // Collection<ExperimentalDesign> eeCol = new HashSet();
        // int testNum = 3;
        // for ( int i = 0; i < testNum; i++ ) {
        // ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        // ed.setName( "Experimental Design " + i );
        // ed.setDescription( i + ": A test experimental design." );
        // eeCol.add( ed );
        // }
        //
        // ee.setExperimentalDesigns( eeCol );
        // ***********
        ArrayDesignService ads = ( ArrayDesignService ) getBean( "arrayDesignService" );
        if ( ads.findArrayDesignByName( ad.getName() ) == null ) ads.findOrCreate( ad );
    }

    /**
     * @throws Exception
     */
    public void testSave() throws Exception {
        log.debug( "testing save" );

        ArrayDesignFormController c = ( ArrayDesignFormController ) getBean( "arrayDesignFormController" );

        request = new MockHttpServletRequest( "POST", "/arrayDesign/editArrayDesign.html" );
        request.addParameter( "name", ad.getName() );
        request.addParameter( "description", ad.getDescription() );

        ModelAndView mav = c.handleRequest( request, ( new MockHttpServletResponse() ) );

        String errorsKey = BindException.ERROR_KEY_PREFIX + c.getCommandName();
        Errors errors = ( Errors ) mav.getModel().get( errorsKey );

        assertNull( errors );
        assertNotNull( request.getSession().getAttribute( "messages" ) );
        assertEquals( "redirect:/arrayDesign/showAllArrayDesigns.html", mav.getViewName() );

    }

    /**
     * @throws Exception
     */
    public void testEdit() throws Exception {
        log.debug( "testing edit" );

        ArrayDesignFormController c = ( ArrayDesignFormController ) getBean( "arrayDesignFormController" );

        request = new MockHttpServletRequest( "GET", "/arrayDesign/editArrayDesign.html" );
        request.addParameter( "name", ad.getName() );

        ModelAndView mav = c.handleRequest( request, ( new MockHttpServletResponse() ) );

        assertEquals( "arrayDesign.edit", mav.getViewName() );
    }
}
