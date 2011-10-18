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
package ubic.gemma.web.controller.expression.arrayDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignFormControllerTest extends BaseSpringWebTest {

    ArrayDesign ad = null;

    @Autowired
    ArrayDesignFormController arrayDesignFormController;

    private MockHttpServletRequest request = null;

    /**
     * setUp
     */
    @Before
    public void setup() throws Exception {
        ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 20 ) );
        ad.setDescription( "An array design created in the ArrayDesignFormControllerTest." );
        ad.setPrimaryTaxon( this.getTaxon( "human" ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Contact c = Contact.Factory.newInstance();
        c.setName( RandomStringUtils.randomAlphabetic( 20 ) );
        ad.setDesignProvider( c );

        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        assert ad.getId() != null;
    }

    /**
     * @throws Exception
     */
    @Test
    public void testEdit() throws Exception {

        request = super.newGet( "/arrays/editArrayDesign.html" );
        request.setParameter( "name", ad.getName() );
        request.setParameter( "id", ad.getId().toString() );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );

        ModelAndView mav = arrayDesignFormController.handleRequest( request, ( new MockHttpServletResponse() ) );

        assertEquals( "arrayDesign.edit", mav.getViewName() );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testSave() throws Exception {

        request = newPost( "/arrays/editArrayDesign.html" );
        request.setParameter( "name", ad.getName() );
        request.setParameter( "description", ad.getDescription() );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );
        request.setParameter( "id", ad.getId().toString() );
        ModelAndView mav = arrayDesignFormController.handleRequest( request, ( new MockHttpServletResponse() ) );

        String errorsKey = BindingResult.MODEL_KEY_PREFIX + arrayDesignFormController.getCommandName();
        Errors errors = ( Errors ) mav.getModel().get( errorsKey );

        assertNull( "Got errors:" + errors, errors );
        assertNotNull( request.getSession().getAttribute( "messages" ) );
        assertTrue( mav.getView() instanceof RedirectView );
        assertEquals( "/Gemma/arrays/showArrayDesign.html?id=" + ad.getId(),
                ( ( RedirectView ) mav.getView() ).getUrl() );

    }

    @After
    public void tearDown() throws Exception {
        if ( ad != null ) {
            ArrayDesignService ads = ( ArrayDesignService ) getBean( "arrayDesignService" );
            ads.remove( ad );
        }
    }
}
