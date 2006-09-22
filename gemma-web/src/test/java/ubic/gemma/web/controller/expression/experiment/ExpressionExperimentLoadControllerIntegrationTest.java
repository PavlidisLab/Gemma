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

import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentLoadControllerIntegrationTest extends AbstractGeoServiceTest {
    protected static final String GEO_TEST_DATA_ROOT = "/gemma-core/src/test/resources/data/loader/expression/geo/";

    private ExpressionExperimentLoadController controller;

    AbstractGeoService geoService;

    protected void init() {
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
    }

    /**
     * @throws Exception
     */
    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        controller = ( ExpressionExperimentLoadController ) getBean( "expressionExperimentLoadController" );
        this.init();
    }

    public MockHttpServletRequest newPost( String url ) {
        return new MockHttpServletRequest( "POST", url );
    }

    public MockHttpServletRequest newGet( String url ) {
        return new MockHttpServletRequest( "GET", url );
    }

    /**
     * Test method for
     * {@link ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)}.
     */
    public final void testOnSubmit() throws Exception {
        String path = getTestFileBasePath();

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/loadExpressionExperiment.html" );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds999Short" ) );
        controller.setGeoDatasetService( ( GeoDatasetService ) geoService );

        request.setParameter( "accession", "GDS999" );
        request.setParameter( "loadPlatformOnly", "false" );
        request.setRemoteUser( "test" );
        ModelAndView mv = controller.handleRequest( request, response );

        assertEquals( "Wrong view", "expressionExperiment.detail", mv.getViewName() );

    }

    // GDS395 - same platform as GDS999 XX GDS395 is defective.
    // GDS266 - use instead as an example. linked to GDS267, which uses the B array.
    public final void testOnSubmitB() throws Exception {
        String path = getTestFileBasePath();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/loadExpressionExperiment.html" );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds266Short" ) );
        controller.setGeoDatasetService( ( GeoDatasetService ) geoService );
        request.setParameter( "accession", "GDS266" );
        request.setParameter( "loadPlatformOnly", "false" );
        request.setRemoteUser( "test" );
        ModelAndView mv = controller.handleRequest( request, response );

        assertEquals( "Wrong view", "expressionExperiment.detail", mv.getViewName() );

    }

    /**
     * Cases to test:
     * <ul>
     * <li>New platform, new expression experiment
     * <li>Existing platform, new expression experiment, but platform is not filled in.
     * <li>Existing platform, new expression experiment.
     * <li>Existing platform, existing expression experiment
     * </ul>
     */

    public final void testShowForm() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newGet( "/loadExpressionExperiment.html" );
        request.setParameter( "accession", "GDS999" );
        request.setParameter( "loadPlatformOnly", "false" );
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "Returned incorrect view name", "loadExpressionExperimentForm", mv.getViewName() );

    }

}