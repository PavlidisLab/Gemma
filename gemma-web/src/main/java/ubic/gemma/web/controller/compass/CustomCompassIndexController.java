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
package ubic.gemma.web.controller.compass;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.compass.spring.web.mvc.CompassIndexCommand;
import org.compass.spring.web.mvc.CompassIndexResults;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingCompassIndexController;

/**
 * A general Spring's MVC Controller that perform the index operation of <code>CompassGps</code>. The indexing here
 * rebuilds the database. That is, the index is deleted, then rebuilt (at which time the indicies exist but are empty),
 * and then updated.
 * <p>
 * Will perform the index operation if the {@link org.compass.spring.web.mvc.CompassIndexCommand} <code>doIndex</code>
 * property is set to true.
 * <p>
 * The controller has two views to be set, the <code>indexView</code>, which is the view that holds the screen which
 * the user will initiate the index operation, and the <code>indexResultsView</code>, which will show the results of
 * the index operation.
 * <p>
 * The results of the index operation will be saved under the <code>indexResultsName</code>, which defaults to
 * "indexResults".
 * 
 * todo:  I don't like the way this controller works at all.  Plan to make this genaric like all of our other controllers
 * use spring injection to get the different compassGps beans and just make it simpler.  No need to use compass's index controller classes.
 * 
 * @author kimchy
 * @author keshav
 * @version $Id$
 * @spring.bean id="indexController"
 * @spring.property name = "compassGps" ref="expressionGps"
 * @spring.property name = "indexView" value="indexer"
 * @spring.property name = "indexResultsView" value="indexer"
 * @spring.property name = "taskRunningService" ref="taskRunningService"
 * @spring.property name = "messageUtil" ref="messageUtil"
 */

public class CustomCompassIndexController extends BackgroundProcessingCompassIndexController {

    private Log log = LogFactory.getLog( CustomCompassIndexController.class );

    private String indexView;

    private String indexResultsView;

    private String indexResultsName = "indexResults";

    public CustomCompassIndexController() {
        setCommandClass( CompassIndexCommand.class );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if ( indexView == null ) {
            throw new IllegalArgumentException( "Must set the indexView property" );
        }
        if ( indexResultsView == null ) {
            throw new IllegalArgumentException( "Must set hte indexResultsView property" );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractCommandController#handle(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    @SuppressWarnings("unused")
    protected ModelAndView handle( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        CompassIndexCommand indexCommand = ( CompassIndexCommand ) command;
        
        if ( !StringUtils.hasText( indexCommand.getDoIndex() )) {
            return new ModelAndView( getIndexView(), getCommandName(), indexCommand );
        }

        
        IndexJob index;
        
        if (indexCommand.getDoIndex().equalsIgnoreCase( "genes" )) {
            index = new IndexJob( request,
                    ( CompassIndexCommand ) command, ( CompassGpsInterfaceDevice ) getWebApplicationContext().getBean(
                            "geneGps" ) );         
        }
        else if  (indexCommand.getDoIndex().equalsIgnoreCase( "ee" ) ) {
         index = new IndexJob( request,
                ( CompassIndexCommand ) command, ( CompassGpsInterfaceDevice ) getWebApplicationContext().getBean(
                        "expressionGps" ) );
        }
        else if  (indexCommand.getDoIndex().equalsIgnoreCase( "ad" ) ) {
            index = new IndexJob( request,
                   ( CompassIndexCommand ) command, ( CompassGpsInterfaceDevice ) getWebApplicationContext().getBean(
                           "arrayGps" ) );
        }
        else {
            log.warn( "Indexcontroller had a button pressed but no appropirate value was found in the request." );
            return new ModelAndView( getIndexView(), getCommandName(), indexCommand );        
        }
        
        String taskId = startJob( request, index );

        return new ModelAndView( new RedirectView( "processProgress.html?taskid=" + taskId ) );
    }

    /**
     * <hr>
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$ This inner class is used
     *          for creating a seperate thread that will delete the compass ee index
     */
    class IndexJob extends BackgroundControllerJob<ModelAndView> {

        private CompassIndexCommand indexCommand;
        private CompassGpsInterfaceDevice gpsDevice;

        public IndexJob( HttpServletRequest request, CompassIndexCommand indexCommand,
                CompassGpsInterfaceDevice gpsDevice ) {
            super( request );
            this.indexCommand = indexCommand;
            this.gpsDevice = gpsDevice;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Attempting to index" );

            long time = System.currentTimeMillis();

            log.info( "Rebuilding compass index." );
            //CompassUtils.rebuildCompassIndex( gpsDevice );
            gpsDevice.index();
            time = System.currentTimeMillis() - time;
            CompassIndexResults indexResults = new CompassIndexResults( time );
            Map<Object, Object> data = new HashMap<Object, Object>();
            data.put( getCommandName(), indexCommand );
            data.put( getIndexResultsName(), indexResults );

            ProgressManager.destroyProgressJob( job );

            return new ModelAndView( getIndexResultsView(), data );

        }
    }

    /**
     * <hr>
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$ Used for creating a
     *          seperate thread in rebuilding the Gene's index
     */

    class IndexGenesJob extends BackgroundControllerJob<ModelAndView> {

        private CompassIndexCommand indexCommand;
        private CompassGpsInterfaceDevice gpsDevice;

        public IndexGenesJob( HttpServletRequest request, CompassIndexCommand indexCommand,
                CompassGpsInterfaceDevice gpsDevice ) {
            super( request );
            this.indexCommand = indexCommand;
            this.gpsDevice = gpsDevice;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Attempting to index Genes in Database" );

            long time = System.currentTimeMillis();

            log.info( "Rebuilding gene index." );
            CompassUtils.rebuildCompassIndex( gpsDevice );

            time = System.currentTimeMillis() - time;
            CompassIndexResults indexResults = new CompassIndexResults( time );
            Map<Object, Object> data = new HashMap<Object, Object>();
            data.put( getCommandName(), indexCommand );
            data.put( getIndexResultsName(), indexResults );

            ProgressManager.destroyProgressJob( job );

            return new ModelAndView( getIndexResultsView(), data );

        }
    }

    /**
     * Returns the view that holds the screen which the user will initiate the index operation.
     */
    public String getIndexView() {
        return indexView;
    }

    /**
     * Sets the view that holds the screen which the user will initiate the index operation.
     */
    public void setIndexView( String indexView ) {
        this.indexView = indexView;
    }

    /**
     * Returns the name of the results that the {@link CompassIndexResults} will be saved under. Defaults to
     * "indexResults".
     */
    public String getIndexResultsName() {
        return indexResultsName;
    }

    /**
     * Sets the name of the results that the {@link CompassIndexResults} will be saved under. Defaults to
     * "indexResults".
     * 
     * @param indexResultsName
     */
    public void setIndexResultsName( String indexResultsName ) {
        this.indexResultsName = indexResultsName;
    }

    /**
     * Returns the view which will show the results of the index operation.
     */
    public String getIndexResultsView() {
        return indexResultsView;
    }

    /**
     * Sets the view which will show the results of the index operation.
     * 
     * @param indexResultsView
     */
    public void setIndexResultsView( String indexResultsView ) {
        this.indexResultsView = indexResultsView;
    }

}
