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

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignFormControllerTest extends BaseSpringContextTest {

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

        ArrayDesignService ads = ( ArrayDesignService ) getBean( "arrayDesignService" );
        ad = ads.create( ad );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        ArrayDesignService ads = ( ArrayDesignService ) getBean( "arrayDesignService" );
        if ( ad != null ) {
            ads.remove( ad );
        }
    }

    /**
     * @throws Exception
     */
    public void testSave() throws Exception {
        ArrayDesignFormController c = ( ArrayDesignFormController ) getBean( "arrayDesignFormController" );

        request = new MockHttpServletRequest( "POST", "/arrays/editArrayDesign.html" );
        request.addParameter( "name", ad.getName() );
        request.addParameter( "description", ad.getDescription() );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );

        ModelAndView mav = c.handleRequest( request, ( new MockHttpServletResponse() ) );

        String errorsKey = BindException.ERROR_KEY_PREFIX + c.getCommandName();
        Errors errors = ( Errors ) mav.getModel().get( errorsKey );

        assertNull( errors );
        assertNotNull( request.getSession().getAttribute( "messages" ) );
        assertEquals( "redirect:/arrays/showAllArrayDesigns.html", mav.getViewName() );

    }

    /**
     * @throws Exception
     */
    public void testEdit() throws Exception {
        ArrayDesignFormController c = ( ArrayDesignFormController ) getBean( "arrayDesignFormController" );

        request = new MockHttpServletRequest( "GET", "/arrays/editArrayDesign.html" );
        request.addParameter( "name", ad.getName() );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );

        ModelAndView mav = c.handleRequest( request, ( new MockHttpServletResponse() ) );

        assertEquals( "arrayDesign.edit", mav.getViewName() );
    }
}
