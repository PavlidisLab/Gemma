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

import ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask;
import ubic.gemma.javaspaces.gigaspaces.GigaSpacesResult;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController;
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
 * @spring.property name="gigaSpacesUtil" ref="gigaSpacesUtil"
 */
public class ExpressionExperimentLoadController extends AbstractGigaSpacesFormController {

    GeoDatasetService geoDatasetService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        return startJob( command, GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl(),
                ExpressionExperimentTask.class.getName(), true );
    }

    public String run( ExpressionExperimentLoadCommand command ) {
        return run( command, GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentTask.class.getName(),
                true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#setGigaSpacesUtil(ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil)
     */
    @Override
    public void setGigaSpacesUtil( GigaSpacesUtil gigaSpacesUtil ) {
        this.injectGigaspacesUtil( gigaSpacesUtil );
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
            Object command, MessageUtil messenger ) {

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, command, messenger ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
                Map<Object, Object> model = new HashMap<Object, Object>();
                ExpressionExperimentLoadCommand expressionExperimentLoadCommand = ( ( ExpressionExperimentLoadCommand ) command );

                String accesionNum = expressionExperimentLoadCommand.getAccession();

                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Loading " + expressionExperimentLoadCommand.getAccession() );

                // put the accession number in a safer form
                accesionNum = StringUtils.strip( accesionNum );
                accesionNum = StringUtils.upperCase( accesionNum );

                log.info( "Loading " + accesionNum );

                if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                    geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
                }

                boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
                String list = "";
                if ( expressionExperimentLoadCommand.isLoadPlatformOnly() ) {
                    job.updateProgress( "Loading platforms only." );
                    Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accesionNum, true,
                            doSampleMatching );
                    this.saveMessage( "Successfully loaded " + arrayDesigns.size() + " array designs" );
                    model.put( "arrayDesigns", arrayDesigns );
                    ProgressManager.destroyProgressJob( job );

                    if ( arrayDesigns.size() == 1 ) {
                        return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id="
                                + arrayDesigns.iterator().next().getId() ) );
                    } else {
                        for ( ArrayDesign ad : arrayDesigns )
                            list += ad.getId() + ",";
                        return new ModelAndView(
                                new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) );
                    }

                } else {
                    Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accesionNum, false,
                            doSampleMatching );
                    if ( result.size() == 1 ) {
                        ExpressionExperiment loaded = result.iterator().next();
                        this.saveMessage( "Successfully loaded " + loaded );
                        model.put( "expressionExperiment", loaded );
                        ProgressManager.destroyProgressJob( job );
                        return new ModelAndView( new RedirectView(
                                "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                        + result.iterator().next().getId() ) );
                    } else {
                        // model.put( "expressionExeriments", result );
                        this.saveMessage( "Successfully loaded " + result.size() + " expression experiments" );
                        ProgressManager.destroyProgressJob( job );
                        for ( ExpressionExperiment ee : result )
                            list += ee.getId() + ",";
                        return new ModelAndView( new RedirectView(
                                "/Gemma/expressionExperiment/showAllExpressionExperiments.html?ids=" + list ) );
                    }

                }

            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#getSpaceRunner(java.lang.String,
     *      org.acegisecurity.context.SecurityContext, javax.servlet.http.HttpServletRequest, java.lang.Object,
     *      ubic.gemma.web.util.MessageUtil)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String taskId, SecurityContext securityContext,
            Object command, MessageUtil messenger ) {

        final ExpressionExperimentTask eeTaskProxy = ( ExpressionExperimentTask ) updatedContext.getBean( "proxy" );

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, command, messenger ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
                Map<Object, Object> model = new HashMap<Object, Object>();
                ExpressionExperimentLoadCommand expressionExperimentLoadCommand = ( ( ExpressionExperimentLoadCommand ) command );

                String accesionNum = expressionExperimentLoadCommand.getAccession();

                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Loading " + expressionExperimentLoadCommand.getAccession() );

                // put the accession number in a safer form
                accesionNum = StringUtils.strip( accesionNum );
                accesionNum = StringUtils.upperCase( accesionNum );

                log.info( "Loading " + accesionNum );

                if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                    geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
                }

                boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
                String list = "";
                if ( expressionExperimentLoadCommand.isLoadPlatformOnly() ) {
                    job.updateProgress( "Loading platforms only." );
                    Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accesionNum, true,
                            doSampleMatching );
                    this.saveMessage( "Successfully loaded " + arrayDesigns.size() + " array designs" );
                    model.put( "arrayDesigns", arrayDesigns );
                    ProgressManager.destroyProgressJob( job );

                    if ( arrayDesigns.size() == 1 ) {
                        return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id="
                                + arrayDesigns.iterator().next().getId() ) );
                    } else {
                        for ( ArrayDesign ad : arrayDesigns )
                            list += ad.getId() + ",";
                        return new ModelAndView(
                                new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) );
                    }

                } else {
                    GigaSpacesResult res = eeTaskProxy.execute( accesionNum, false, doSampleMatching );
                    Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) res.getAnswer();
                    log.info( "result " + result );

                    if ( result.size() == 1 ) {
                        ExpressionExperiment loaded = result.iterator().next();
                        this.saveMessage( "Successfully loaded " + loaded );
                        model.put( "expressionExperiment", loaded );
                        ProgressManager.destroyProgressJob( job );
                        return new ModelAndView( new RedirectView(
                                "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                        + result.iterator().next().getId() ) );
                    } else {
                        // model.put( "expressionExeriments", result );
                        this.saveMessage( "Successfully loaded " + result.size() + " expression experiments" );
                        ProgressManager.destroyProgressJob( job );
                        for ( ExpressionExperiment ee : result )
                            list += ee.getId() + ",";
                        return new ModelAndView( new RedirectView(
                                "/Gemma/expressionExperiment/showAllExpressionExperiments.html?ids=" + list ) );
                    }

                }

            }
        };
    }
}
