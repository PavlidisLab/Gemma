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

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
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
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand;
import ubic.gemma.web.util.MessageUtil;

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
 * @author kimchy
 * @author keshav
 * @version $Id$
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

        if ( !StringUtils.hasText( indexCommand.getDoIndex() ) || !indexCommand.getDoIndex().equalsIgnoreCase( "true" ) ) {
            return new ModelAndView( getIndexView(), getCommandName(), indexCommand );
        }

        String taskId = startJob( command, request );
        return new ModelAndView( new RedirectView( "processProgress.html?taskid=" + taskId ) );
    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            HttpServletRequest request, Object command, MessageUtil messenger ) {
        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, request, command, messenger ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Attempting to index EE Database" );

                long time = System.currentTimeMillis();

                log.info( "Rebuilding compass index." );
                CompassUtils.rebuildCompassIndex( ( CompassGpsInterfaceDevice ) getWebApplicationContext().getBean(
                        "compassGps" ) );

                CompassIndexCommand indexCommand = ( CompassIndexCommand ) command;

                time = System.currentTimeMillis() - time;
                CompassIndexResults indexResults = new CompassIndexResults( time );
                Map<Object, Object> data = new HashMap<Object, Object>();
                data.put( getCommandName(), indexCommand );
                data.put( getIndexResultsName(), indexResults );
                
                ProgressManager.destroyProgressJob( job );
                return new ModelAndView( getIndexResultsView(), data );
            }
        };

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
