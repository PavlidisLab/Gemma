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
import org.compass.spring.web.mvc.CompassIndexResults;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.MultiBackgroundProcessingController;

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
 * @author keshav
 * @version $Id$
 * @spring.bean id="indexController"
 * @spring.property name="arrayGps" ref="arrayGps"
 * @spring.property name="expressionGps" ref="expressionGps"
 * @spring.property name="geneGps" ref="geneGps"
 * @spring.property name="ontologyGps" ref="ontologyGps"
 * @spring.property name="formView" value="indexer"
 * @spring.property name="successView" value="indexer"
 */

public class CustomCompassIndexController extends MultiBackgroundProcessingController {

    private Log log = LogFactory.getLog( CustomCompassIndexController.class );

    private CompassGpsInterfaceDevice expressionGps;
    private CompassGpsInterfaceDevice arrayGps;
    private CompassGpsInterfaceDevice geneGps;
    private CompassGpsInterfaceDevice ontologyGps;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        IndexJob index;

        if ( StringUtils.hasText( request.getParameter( "geneIndex" ) ) ) {
            index = new IndexJob( request, geneGps, "Gene Index" );
        } else if ( StringUtils.hasText( request.getParameter( "eeIndex" ) ) ) {
            index = new IndexJob( request, expressionGps, "Dataset Index" );
        } else if ( StringUtils.hasText( request.getParameter( "arrayIndex" ) ) ) {
            index = new IndexJob( request, arrayGps, "Array Index" );
        } else if ( StringUtils.hasText( request.getParameter( "ontologyIndex" ) ) )
            index = new IndexJob( request, ontologyGps, "Ontology Index" );
        else
            return new ModelAndView( this.getFormView() );

        return startJob( request, index );

    }

    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        if ( request.getParameter( "cancel" ) != null ) {
            this.saveMessage( request, "Cancelled Index" );
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "geneIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "eeIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "arrayIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "ontologyIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }

        return super.showForm( request, response, errors );
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }

    /**
     * @author klc
     * @version $Id$ This inner class is used
     *          for creating a seperate thread that will delete the compass ee index
     */
    class IndexJob extends BackgroundControllerJob<ModelAndView> {

        private CompassGpsInterfaceDevice gpsDevice;
        private String description;

        public IndexJob( HttpServletRequest request, CompassGpsInterfaceDevice gpsDevice, String description ) {
            super( getMessageUtil() );
            this.gpsDevice = gpsDevice;
            this.description = description;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Attempting to index" );

            long time = System.currentTimeMillis();

            job.updateProgress( "Preparing to rebuild " + this.description );
            log.info( "Preparing to rebuild " + this.description );

            CompassUtils.rebuildCompassIndex( gpsDevice );
            time = System.currentTimeMillis() - time;
            CompassIndexResults indexResults = new CompassIndexResults( time );
            Map<Object, Object> data = new HashMap<Object, Object>();
            data.put( "indexResults", indexResults );

            ProgressManager.destroyProgressJob( job );

            ModelAndView mv = new ModelAndView( "indexer" );
            mv.addObject( "time", time );
            mv.addObject( "description", this.description );

            return mv;

        }
    }

    /**
     * @return the arrayGps
     */
    public CompassGpsInterfaceDevice getArrayGps() {
        return arrayGps;
    }

    /**
     * @param arrayGps the arrayGps to set
     */
    public void setArrayGps( CompassGpsInterfaceDevice arrayGps ) {
        this.arrayGps = arrayGps;
    }

    /**
     * @return the expressionGps
     */
    public CompassGpsInterfaceDevice getExpressionGps() {
        return expressionGps;
    }

    /**
     * @param expressionGps the expressionGps to set
     */
    public void setExpressionGps( CompassGpsInterfaceDevice expressionGps ) {
        this.expressionGps = expressionGps;
    }

    /**
     * @return the geneGps
     */
    public CompassGpsInterfaceDevice getGeneGps() {
        return geneGps;
    }

    /**
     * @param geneGps the geneGps to set
     */
    public void setGeneGps( CompassGpsInterfaceDevice geneGps ) {
        this.geneGps = geneGps;
    }

    /**
     * @return the ontologyGps
     */
    public CompassGpsInterfaceDevice getOntologyGps() {
        return ontologyGps;
    }

    /**
     * @param ontologyGps the ontologyGps to set
     */
    public void setOntologyGps( CompassGpsInterfaceDevice ontologyGps ) {
        this.ontologyGps = ontologyGps;
    }

}
