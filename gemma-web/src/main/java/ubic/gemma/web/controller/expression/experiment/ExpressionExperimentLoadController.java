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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionExperimentLoadController"
 * @spring.property name="commandName" value="expressionExperimentLoadCommand"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand"
 * @spring.property name="validator" ref="genericBeanValidator"
 * @spring.property name="formView" value="loadExpressionExperimentForm"
 * @spring.property name="successView" value="loadExpressionExperimentProgress.html"
 * @spring.property name="geoDatasetService" ref="geoDatasetService"
 */
public class ExpressionExperimentLoadController extends BaseFormController {

    private static Log log = LogFactory.getLog( ExpressionExperimentLoadController.class.getName() );
    private GeoDatasetService geoDatasetService;

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {


        ExpressionExperimentLoadCommand eeLoadCommand = ( ExpressionExperimentLoadCommand ) command;

        // validate an accession was entered (FIXME - should be done by validation
        if ( StringUtils.isBlank( eeLoadCommand.getAccession() ) ) {
            Object[] args = new Object[] { getText( "expressionExperiment.accession", request.getLocale() ) };
            errors.rejectValue( "accession", "errors.required", args, "Accession" );
            return showForm( request, response, errors );
        }

        final SecurityContext context = SecurityContextHolder.getContext(); // all new threads need this
        // to acccess protected
        // resources (like services)
        ProgressJob job = ProgressManager.createProgressJob( SecurityContextHolder.getContext().getAuthentication()
                .getName(), "loading big stuff!" );

        LoadEE eeLoader = new LoadEE( request, response, errors, context, eeLoadCommand, job );

        // Create the thread supplying it with the runnable object
        Thread thread = new Thread( eeLoader );

        // Start the thread
        thread.start();

        ModelAndView mv = new ModelAndView( new RedirectView( "loadExpressionExperimentProgress.html" ) );

        return mv;
    }

    // a inner class for forking the process data into the database;
    class LoadEE implements Runnable {

        HttpServletRequest req;
        HttpServletResponse resp;
        BindException err;
        ExpressionExperimentLoadCommand eeLoadCommand;
        ProgressJob job;
        SecurityContext context;

        public LoadEE( HttpServletRequest request, HttpServletResponse response, BindException errors,
                SecurityContext context, ExpressionExperimentLoadCommand eeLoadCommand, ProgressJob job ) {
            req = request;
            response = resp;
            err = errors;
            this.job = job;
            this.eeLoadCommand = eeLoadCommand;
           this.context = context;
        }

        public void run() {
          
            SecurityContextHolder.setContext( context ); // so that acegi doesn't deny the thread permission
            Map<Object, Object> model = new HashMap<Object, Object>();
            String accesionNum = eeLoadCommand.getAccession();
            
            //put the accession number in a safer form
            accesionNum = StringUtils.strip( accesionNum );
            accesionNum = StringUtils.upperCase( accesionNum );
            
            log.info( "Loading " + accesionNum );
            if ( eeLoadCommand.isLoadPlatformOnly() ) {
                log.info( "Only loading platform" );
            }

            if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            }

            if ( eeLoadCommand.isLoadPlatformOnly() ) {
            
                job.updateProgress( new ProgressData( 0, "Loading AD..." ) );
                geoDatasetService.setLoadPlatformOnly( true );
                Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService
                        .fetchAndLoad( accesionNum );
                model.put( "arrayDesigns", arrayDesigns ); // FIXME view should be different than default.

                job.setForwardingURL( "/Gemma/arrayDesign/showArrayDesign.html?id=" + arrayDesigns.iterator().next().getId() );

            } else {               
                job.updateProgress( new ProgressData( 0, "Loading EE..." ) );

                ExpressionExperiment result = ( ExpressionExperiment ) geoDatasetService.fetchAndLoad( accesionNum );
                model.put( "expressionExperiment", result );
              
                 job.setForwardingURL("/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + result.getId() );
            }

            ProgressManager.destroyProgressJob( job );

            // req.setAttribute( "finishing", "true" );
            // try {
            // onSubmit( req, resp, model, err );
            // } catch ( Exception e ) {
            // throw new RuntimeException( e );
            // }

        }

    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param geoDatasetService the geoDatasetService to set
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
    }

}
