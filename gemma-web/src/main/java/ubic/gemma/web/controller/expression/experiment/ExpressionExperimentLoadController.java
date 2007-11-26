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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentLoadTask;
import ubic.gemma.grid.javaspaces.expression.experiment.SpacesExpressionExperimentLoadCommand;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.grid.javaspaces.SpacesUtil;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.gemmaspaces.AbstractSpacesFormController;
import ubic.gemma.web.propertyeditor.ArrayDesignPropertyEditor;
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
 * @spring.property name="gemmaSpacesUtil" ref="gemmaSpacesUtil"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="arrayExpressLoadService" ref="arrayExpressLoadService"
 */
public class ExpressionExperimentLoadController extends AbstractSpacesFormController {

    GeoDatasetService geoDatasetService;
    ArrayDesignService arrayDesignService;
    ArrayExpressLoadService arrayExpressLoadService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        ExpressionExperimentLoadCommand command = new ExpressionExperimentLoadCommand();
        return command;
    }

    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( ArrayDesign.class, new ArrayDesignPropertyEditor( this.arrayDesignService ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) throws Exception {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        populateArrayDesignReferenceData( mapping );
        return mapping;

    }

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
        return startJob( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentLoadTask.class
                .getName(), true );
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param command
     * @return
     */
    public String run( ExpressionExperimentLoadCommand command ) {
        return run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentLoadTask.class.getName(),
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
            cancel( request );
            this.saveMessage( request, "Cancelled processing" );
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateArrayDesignReferenceData( Map<String, List<? extends Object>> mapping ) {
        List<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
        for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) arrayDesignService.loadAll() ) {
            // remove AD's that are mergees or subsumers

            if ( arrayDesign.getSubsumingArrayDesign() != null ) continue;

            if ( arrayDesign.getMergedInto() != null ) continue;

            arrayDesigns.add( arrayDesign );
        }
        Collections.sort( arrayDesigns, new Comparator<ArrayDesign>() {
            public int compare( ArrayDesign o1, ArrayDesign o2 ) {
                return ( o1 ).getName().compareTo( ( o2 ).getName() );
            }
        } );

        mapping.put( "arrayDesigns", arrayDesigns );
    }

    /**
     * This method has been deprecated in favor of an ajax call.
     * 
     * @param request
     * @deprecated
     */
    private void cancel( HttpServletRequest request ) {
        Future job = ( Future ) request.getSession().getAttribute( JOB_ATTRIBUTE );
        job.cancel( true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#setGigaSpacesUtil(ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil)
     */
    @Override
    public void setGemmaSpacesUtil( SpacesUtil gemmaSpacesUtil ) {
        this.injectGemmaSpacesUtil( gemmaSpacesUtil );
    }

    /**
     * @param geoDatasetService the geoDatasetService to set
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param arrayExpressLoadService the arrayExpressLoadService to set
     */
    public void setArrayExpressLoadService( ArrayExpressLoadService arrayExpressLoadService ) {
        this.arrayExpressLoadService = arrayExpressLoadService;
    }

    // TODO: getRunner and getSpaceRunner have a lot of similar code, can we consolidate this?
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
                boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
                String list = "";
                if ( expressionExperimentLoadCommand.isLoadPlatformOnly() ) {
                    job.updateProgress( "Loading platforms only." );
                    Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accesionNum, true,
                            doSampleMatching, aggressiveQtRemoval );
                    this.saveMessage( "Successfully loaded " + arrayDesigns.size() + " array designs" );
                    model.put( "arrayDesigns", arrayDesigns );

                    if ( arrayDesigns.size() == 1 ) {
                        return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id="
                                + arrayDesigns.iterator().next().getId() ) );
                    } else {
                        for ( ArrayDesign ad : arrayDesigns )
                            list += ad.getId() + ",";
                        return new ModelAndView(
                                new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) );
                    }
                } else if ( expressionExperimentLoadCommand.isArrayExpress() ) {

                    if ( expressionExperimentLoadCommand.getArrayDesignName() == null ) {
                        this.saveMessage( "Unable to load: Must select an array design to use" );
                        return new ModelAndView( new RedirectView( "/Gemma/loadExpressionExperiment.html" ) );
                    }

                    ExpressionExperiment result = arrayExpressLoadService.load( accesionNum,
                            expressionExperimentLoadCommand.getArrayDesignName() );

                    this.saveMessage( "Successfully loaded " + result.getName() );
                    model.put( "expressionExperiment", result );
                    return new ModelAndView( new RedirectView(
                            "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + result.getId() ) );
                } else /* GEO */{
                    Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accesionNum, false,
                            doSampleMatching, aggressiveQtRemoval );
                    if ( result.size() == 1 ) {
                        ExpressionExperiment loaded = result.iterator().next();
                        this.saveMessage( "Successfully loaded " + loaded );
                        model.put( "expressionExperiment", loaded );
                        return new ModelAndView( new RedirectView(
                                "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                        + result.iterator().next().getId() ) );
                    } else {
                        this.saveMessage( "Successfully loaded " + result.size() + " expression experiments" );
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

        final ExpressionExperimentLoadTask eeTaskProxy = ( ExpressionExperimentLoadTask ) updatedContext
                .getBean( "proxy" );

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
                boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
                String list = "";
                if ( expressionExperimentLoadCommand.isLoadPlatformOnly() ) {
                    job.updateProgress( "Loading platforms only." );
                    Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accesionNum, true,
                            doSampleMatching, aggressiveQtRemoval );
                    this.saveMessage( "Successfully loaded " + arrayDesigns.size() + " array designs" );
                    model.put( "arrayDesigns", arrayDesigns );
                    // ProgressManager.destroyProgressJob( job, !AJAX );

                    if ( arrayDesigns.size() == 1 ) {
                        return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id="
                                + arrayDesigns.iterator().next().getId() ) );
                    } else {
                        for ( ArrayDesign ad : arrayDesigns )
                            list += ad.getId() + ",";
                        return new ModelAndView(
                                new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) );
                    }
                }

                // Array Express
                else if ( expressionExperimentLoadCommand.isArrayExpress() ) {
                    ExpressionExperiment result = arrayExpressLoadService.load( accesionNum,
                            expressionExperimentLoadCommand.getArrayDesignName() );

                    this.saveMessage( "Successfully loaded " + result.getName() );
                    model.put( "expressionExperiment", result );
                    return new ModelAndView( new RedirectView(
                            "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + result.getId() ) );
                    // GEO
                } else {
                    ExpressionExperimentLoadCommand eeLoadCommand = ( ExpressionExperimentLoadCommand ) command;
                    SpacesExpressionExperimentLoadCommand jsCommand = new SpacesExpressionExperimentLoadCommand(
                            taskId, eeLoadCommand.isLoadPlatformOnly(), eeLoadCommand.isSuppressMatching(),
                            eeLoadCommand.getAccession(), eeLoadCommand.isAggressiveQtRemoval() );

                    SpacesResult res = eeTaskProxy.execute( jsCommand );
                    Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) res.getAnswer();
                    log.info( "result " + result );

                    if ( result.size() == 1 ) {
                        ExpressionExperiment loaded = result.iterator().next();
                        this.saveMessage( "Successfully loaded " + loaded );
                        model.put( "expressionExperiment", loaded );
                        // ProgressManager.destroyProgressJob( job, !AJAX );
                        return new ModelAndView( new RedirectView(
                                "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                        + result.iterator().next().getId() ) );
                    } else {
                        // model.put( "expressionExeriments", result );
                        this.saveMessage( "Successfully loaded " + result.size() + " expression experiments" );
                        // ProgressManager.destroyProgressJob( job, !AJAX );
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
