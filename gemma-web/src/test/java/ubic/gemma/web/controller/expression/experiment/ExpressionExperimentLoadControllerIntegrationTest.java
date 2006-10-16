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

import java.util.Observable;
import java.util.Observer;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressManager;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentLoadControllerIntegrationTest extends AbstractGeoServiceTest {
    protected static final String GEO_TEST_DATA_ROOT = "/gemma-core/src/test/resources/data/loader/expression/geo/";
    private static final long TIMEOUT = 60000; // 60 second timeout

    private ExpressionExperimentLoadController controller;
    ExpressionExperiment ee = null;

    ArrayDesign ad;
    AbstractGeoService geoService;

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

        controller.handleRequest( request, response );

        ProgressData finalPd = monitorLoad();

        String forwardURL = finalPd.getForwardingURL().trim();

        assert ( forwardURL.startsWith( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" ) );

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

        controller.handleRequest( request, response );

        ProgressData finalPd = monitorLoad();
        String forwardURL = finalPd.getForwardingURL().trim();

        // forwardURL.getChars( srcBegin, srcEnd, dst, dstBegin )forwardURL.charAt('=');
        // long id = forwardURL. todo: get the id of the EE and load it to really see if it worked. Can get the id from
        // end of the fowarding url

        assert ( forwardURL.startsWith( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" ) );

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

    /**
     * monitors the a loading progress that was started and returns after 60 seconds or when the data is finished
     * loading
     * 
     * @return ProgressData the last progress data that the load sent
     */
    private ProgressData monitorLoad() {

        // Need to wait to see if the expression experiment loaded correctly.
        // But as the load controller runs in a serpate thread, it will return before its done.
        MockClient mc = new MockClient();
        long start = System.currentTimeMillis();
        long elapsed = 0;
        boolean done = false;

       //Need a short pause to make sure the job is started before we try and monitor it
        try {
            long numMillisecondsToSleep = 3000; // 3 seconds
            Thread.sleep( numMillisecondsToSleep );
        } catch ( InterruptedException e ) {
        }

        // fixme: I'm not sure why the user is set to 'test'. If this changes this test will break
        ProgressManager.addToNotification( "test", mc );

        while ( !done && !( TIMEOUT < elapsed ) ) {
            if ( mc.getProgressData() != null ) {
                done = mc.getProgressData().isDone();
                log.info( mc.getProgressData().getDescription() );
                log.info( "Elapsed time: " + elapsed );
            }

            elapsed = System.currentTimeMillis() - start;
        }

        // forwardURL.getChars( srcBegin, srcEnd, dst, dstBegin )forwardURL.charAt('=');
        // long id = forwardURL. todo: get the id of the EE and load it to really see if it worked. Can get the id from
        // end of the fowarding url

        assert ( done );
        return mc.getProgressData();
    }

    /**
     * <hr>
     * Just a mock client inner class to ease testing
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$
     */

    class MockClient implements Observer {

        private int update;
        private ProgressData pData;

        public MockClient() {
            super();
            this.update = 0;
        }

        @SuppressWarnings("unused")
        public void update( Observable o, Object pd ) {
            pData = ( ProgressData ) pd;
            update++;
        }

        public int upDateTimes() {
            return this.update;

        }

        public ProgressData getProgressData() {
            return pData;
        }

    }

}