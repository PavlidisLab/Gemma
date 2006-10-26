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
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;
import ubic.gemma.web.util.MessageUtil;

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
public class ExpressionExperimentLoadController extends BackgroundProcessingFormController {

    GeoDatasetService geoDatasetService;

    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        String taskId = startJob( command, request );
        return new ModelAndView( new RedirectView( "processProgress.html?taskid=" + taskId ) );
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            Future job = ( Future ) request.getSession().getAttribute( JOB_ATTRIBUTE );
            job.cancel( true );
            this.saveMessage( request, "Cancelled processing" );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseBackgroundProcessingFormController#getRunner(org.acegisecurity.context.SecurityContext,
     *      java.lang.Object, java.lang.String)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            HttpServletRequest request, Object command, MessageUtil messenger ) {

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, request, command, messenger ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
                Map<Object, Object> model = new HashMap<Object, Object>();
                String accesionNum = ( ( ExpressionExperimentLoadCommand ) command ).getAccession();

                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Loading "
                        + ( ( ExpressionExperimentLoadCommand ) command ).getAccession() );

                // put the accession number in a safer form
                accesionNum = StringUtils.strip( accesionNum );
                accesionNum = StringUtils.upperCase( accesionNum );

                 log.info( "Loading " + accesionNum );

                if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                    geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
                }

                if ( ( ( ExpressionExperimentLoadCommand ) command ).isLoadPlatformOnly() ) {
                    job.updateProgress( "Loading platforms only." );
                    geoDatasetService.setLoadPlatformOnly( true );
                    Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accesionNum );
                    this.saveMessage( "Successfully loaded " + arrayDesigns.size() + " array designs" );
                    // FIXME just show the ones loaded.
                    model.put( "arrayDesigns", arrayDesigns );

                } else {
                    geoDatasetService.setLoadPlatformOnly( false );
                    Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accesionNum );
                    if ( result.size() == 1 ) {
                        ExpressionExperiment loaded = result.iterator().next();
                        this.saveMessage( "Successfully loaded " + loaded );
                        model.put( "expressionExperiment", loaded );
                    } else {
                        // FIXME should show just the loaded ones.
                        model.put( "expressionExeriments", result );
                        this.saveMessage( "Successfully loaded " + result.size() + " expression experiments" );
                    }
                }

                ProgressManager.destroyProgressJob( job );
                return new ModelAndView( "expressionExperimentDetails", model );
            }
        };
    }
}
