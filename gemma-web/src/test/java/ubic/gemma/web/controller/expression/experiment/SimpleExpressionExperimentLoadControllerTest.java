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

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.testing.BaseTransactionalSpringWebTest;
import ubic.gemma.testing.MockClient;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.progress.ProgressData;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionExperimentLoadControllerTest extends BaseTransactionalSpringWebTest {

    private SimpleExpressionExperimentLoadController controller;

    /**
     * @throws Exception
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        controller = ( SimpleExpressionExperimentLoadController ) getBean( "simpleExpressionExperimentLoadController" );
    }

    public MockHttpServletRequest newPost( String url ) {
        return new MockHttpServletRequest( "POST", url );
    }

    public MockHttpServletRequest newGet( String url ) {
        return new MockHttpServletRequest( "GET", url );
    }

    public final void testShowForm() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newGet( "/loadSimpleExpressionExperiment.html" );
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "Returned incorrect view name", "simpleExpressionExperimentForm", mv.getViewName() );

    }

    public final void testSubmit() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/loadSimpleExpressionExperiment.html" );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );

        request.setParameter( "name", "GDS266-foo" );
        request.setParameter( "description", "Just a test" );
        request.setParameter( "quantitationTypeName", "foo" );
        request.setParameter( "quantitationTypeDescription", "bar" );
        request.setParameter( "type", "MEASURED_SIGNAL" );
        request.setParameter( "scale", "LINEAR" );
        request.setParameter( "taxon", "mouse" );

        ModelAndView mv = controller.handleRequest( request, response );

        ProgressData finalPd = MockClient.monitorLoad();
        String forwardURL = finalPd.getForwardingURL().trim();

        assertTrue( "Returned incorrect forwarding url: " + forwardURL, forwardURL
                .startsWith( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" ) );
    }

}
