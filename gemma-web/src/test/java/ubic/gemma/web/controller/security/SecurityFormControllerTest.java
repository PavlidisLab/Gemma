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
package ubic.gemma.web.controller.security;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author keshav
 * @version $Id$
 */
public class SecurityFormControllerTest extends BaseSpringWebTest {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
    }

    /**
     *
     *
     */
    public void testMakePrivate() {

        SecurityFormController securityFormController = ( SecurityFormController ) this
                .getBean( "securityFormController" );

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newGet( "/securityManager.html" );

        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );
        // request.setAttribute( "type", "profile" );

        SecurityCommand command = new SecurityCommand();
        command.setShortName( "GSETest" );
        command.setSecurableType( "Expression Experiment" );
        command.setMask( "public" );

        ModelAndView mav = null;
        boolean fail = false;
        try {
            mav = securityFormController.onSubmit( request, response, command, null );
        } catch ( Exception e ) {
            fail( e.getMessage() );
            e.printStackTrace();
        } finally {
            assertFalse( fail );
            assertNotNull( mav );
            log.debug( "View is " + mav.getViewName() );
            log.debug( "Model is " + mav.getModel() );
        }

    }

}
