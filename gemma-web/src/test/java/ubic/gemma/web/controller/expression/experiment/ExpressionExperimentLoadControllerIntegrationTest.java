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

import java.util.Map;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.MockClient;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.BackgroundProcessingFormController;
import ubic.gemma.web.controller.TaskCompletionController;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentLoadControllerIntegrationTest extends AbstractGeoServiceTest {
    protected static final String GEO_TEST_DATA_ROOT = "/gemma-core/src/test/resources/data/loader/expression/geo/";

    private ExpressionExperimentLoadController controller;
    ExpressionExperiment ee = null;

    ArrayDesign ad;
    AbstractGeoService geoService;

    @Override
    protected void init() {
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
    }

    /**
     * @throws Exception
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        controller = ( ExpressionExperimentLoadController ) getBean( "expressionExperimentLoadController" );
        this.init();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onTearDown()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        if ( ee != null && ee.getId() != null ) {
            log.info( "Deleting " + ee );
            ExpressionExperimentService service = ( ExpressionExperimentService ) this
                    .getBean( "expressionExperimentService" );
            service.delete( ee );
        }
        if ( ad != null ) {
            ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
            adService.remove( ad );
        }

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
    @SuppressWarnings("unchecked")
    public final void testOnSubmit() throws Exception {
        endTransaction();
        String path = getTestFileBasePath();

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/loadExpressionExperiment.html" );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds999medium" ) );
        controller.setGeoDatasetService( ( GeoDatasetService ) geoService );

        request.setParameter( "accession", "GDS999" );
        request.setParameter( "loadPlatformOnly", "false" );

        // goes to the progress page...
        controller.handleRequest( request, response );

        String taskId = ( String ) request.getAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( "No task Id", taskId );

        MockClient.monitorTask( taskId );

        Thread.sleep( 500 );// make sure it's really done.

        MockHttpServletRequest afterRequest = newPost( "/checkJobProgress.html" );
        afterRequest.getSession().setAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE, taskId );
        TaskCompletionController taskCheckController = ( TaskCompletionController ) this
                .getBean( "taskCompletionController" );

        ModelAndView mv = null;
        long timeout = 60000;
        long startTime = System.currentTimeMillis();
        while ( mv == null ) {
            Thread.sleep( 200 );
            try {
                mv = taskCheckController.handleRequest( afterRequest, response );
            } catch ( Exception e ) {
                assertTrue( e instanceof AlreadyExistsInSystemException );
                return;
            }
            if ( System.currentTimeMillis() - startTime > timeout ) fail( "Test timed out" );
        }

        assertNotNull( mv );
        Map model = mv.getModel();
        ee = ( ExpressionExperiment ) model.get( "expressionExperiment" );
        assertNotNull( "EE was not persistent", ee.getId() );

    }

    // GDS395 - same platform as GDS999 XX GDS395 is defective.
    // GDS266 - use instead as an example. linked to GDS267, which uses the B array.
    @SuppressWarnings("unchecked")
    public final void testOnSubmitB() throws Exception {
        endTransaction();
        String path = getTestFileBasePath();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/loadExpressionExperiment.html" );
        request.setRemoteUser( ConfigUtils.getString( "gemma.admin.user" ) );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds266Short" ) );
        controller.setGeoDatasetService( ( GeoDatasetService ) geoService );
        request.setParameter( "accession", "GDS266" );
        request.setParameter( "loadPlatformOnly", "false" );

        // goes to the progress page...
        controller.handleRequest( request, response );

        String taskId = ( String ) request.getSession().getAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        MockClient.monitorTask( taskId );

        Thread.sleep( 500 );// make sure it's really done.

        MockHttpServletRequest afterRequest = newPost( "/checkJobProgress.html" );
        afterRequest.getSession().setAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE, taskId );
        TaskCompletionController taskCheckController = ( TaskCompletionController ) this
                .getBean( "taskCompletionController" );

        ModelAndView mv = null;
        long timeout = 60000;
        long startTime = System.currentTimeMillis();
        while ( mv == null ) {
            Thread.sleep( 200 );
            try {
                mv = taskCheckController.handleRequest( afterRequest, response );
            } catch ( Exception e ) {
                if ( !( e instanceof AlreadyExistsInSystemException ) ) {
                    log.error( e, e );
                    fail( "Expected  possibly an AlreadyExistsInSystemException but got a " + e.getClass() );
                }
                return; // ok!
            }
            if ( System.currentTimeMillis() - startTime > timeout ) fail( "Test timed out" );
        }

        Map model = mv.getModel();
        ee = ( ExpressionExperiment ) model.get( "expressionExperiment" );
        assertNotNull( "EE was not persistent", ee.getId() );

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
        // request.setParameter( "accession", "GDS999" );
        // request.setParameter( "loadPlatformOnly", "false" );
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "Returned incorrect view name", "loadExpressionExperimentForm", mv.getViewName() );

    }

}