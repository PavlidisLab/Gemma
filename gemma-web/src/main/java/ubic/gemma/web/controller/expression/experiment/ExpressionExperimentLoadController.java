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
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;

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

        ExpressionExperimentLoadCommand eeLoadCommand = ( ExpressionExperimentLoadCommand ) command;

        startJob( eeLoadCommand, "Loading " + eeLoadCommand.getAccession() );

        return new ModelAndView( new RedirectView( "processProgress.html" ) );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseBackgroundProcessingFormController#getRunner(org.acegisecurity.context.SecurityContext,
     *      java.lang.Object, java.lang.String)
     */
    @Override
    protected BackgroundControllerJob getRunner( SecurityContext securityContext, Object command, String jobDescription ) {
        return new ExpressionExperimentLoadRun( securityContext, command, jobDescription );
    }

    @SuppressWarnings("unchecked")
    class ExpressionExperimentLoadRun extends BackgroundControllerJob {

        public ExpressionExperimentLoadRun( SecurityContext securityContext, Object command, String jobDescription ) {
            init( securityContext, command, jobDescription );
        }

        public void run() {

            SecurityContextHolder.setContext( securityContext );
            Map<Object, Object> model = new HashMap<Object, Object>();
            String accesionNum = ( ( ExpressionExperimentLoadCommand ) command ).getAccession();

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
                model.put( "arrayDesigns", arrayDesigns ); // FIXME view should be different than default.

                job.setForwardingURL( "/Gemma/arrays/showAllArrayDesigns.html?id="
                        + arrayDesigns.iterator().next().getId() );

            } else {
                geoDatasetService.setLoadPlatformOnly( false );
                Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accesionNum );
                if ( result.size() == 1 ) {
                    ExpressionExperiment loaded = result.iterator().next();
                    job.setForwardingURL( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                            + loaded.getId() );
                } else {
                    // FIXME should show just the loaded ones.
                    job.setForwardingURL( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" );
                }
            }

            ProgressManager.destroyProgressJob( job );
        }
    }
}
