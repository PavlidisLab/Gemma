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

import java.io.IOException;
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
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.gemmaspaces.AbstractSpacesFormController;
import ubic.gemma.web.propertyeditor.ArrayDesignPropertyEditor;
import ubic.gemma.web.util.MessageUtil;

/**
 * Handles loading of Expression data into the system when the source is GEO or ArrayExpress, via Spring MVC or AJAX,
 * either in the webapp or in a javaspaces grid. The choice depends on how the system and client is configured. In
 * either case the job runs in its own thread, firing a ProgressJob that the client can monitor.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionExperimentLoadController"
 * @spring.property name="commandName" value="expressionExperimentLoadCommand"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand"
 * @spring.property name="validator" ref="genericBeanValidator"
 * @spring.property name="formView" value="loadExpressionExperimentForm"
 * @spring.property name="successView" value="loadExpressionExperimentProgress.html"
 * @spring.property name="geoDatasetService" ref="geoDatasetService"
 * @spring.property name="spacesUtil" ref="spacesUtil"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="arrayExpressLoadService" ref="arrayExpressLoadService"
 * @see ubic.gemma.web.controller.expression.experiment.SimpleExpressionExperimentLoadController for how flat-file data
 *      is loaded.
 */
public class ExpressionExperimentLoadController extends AbstractSpacesFormController {

    GeoDatasetService geoDatasetService;

    ArrayDesignService arrayDesignService;

    ArrayExpressLoadService arrayExpressLoadService;

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
        return startJob( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentLoadTask.class.getName(),
                true );
    }

    /**
     * Main entry point for Spring MVC
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
     * Main entry point for AJAX calls.
     * 
     * @param command
     * @return
     */
    public String run( ExpressionExperimentLoadCommand command ) {
        return run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentLoadTask.class.getName(), true );
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

    /**
     * @param geoDatasetService the geoDatasetService to set
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#setGigaSpacesUtil(ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil)
     */
    @Override
    public void setSpacesUtil( SpacesUtil spacesUtil ) {
        this.injectSpacesUtil( spacesUtil );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        ExpressionExperimentLoadCommand command = new ExpressionExperimentLoadCommand();
        return command;
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

        return new LoadJob( taskId, securityContext, command, messenger );
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
        return new LoadInSpaceJob( taskId, securityContext, command, messenger );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseFormController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
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
     * Job that loads in a javaspace.
     * 
     * @author Paul
     * @version $Id$
     */
    private class LoadInSpaceJob extends LoadJob {

        final ExpressionExperimentLoadTask eeTaskProxy = ( ExpressionExperimentLoadTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public LoadInSpaceJob( String taskId, SecurityContext parentSecurityContext, Object commandObj,
                MessageUtil messenger ) {
            super( taskId, parentSecurityContext, commandObj, messenger );

        }

        @Override
        protected ModelAndView processArrayExpressJob( ExpressionExperimentLoadCommand eeLoadCommand ) {
            SpacesResult result = process( eeLoadCommand );
            return super.processArrayExpressResult( ( ExpressionExperiment ) result.getAnswer() );
        }

        /**
         * @param eeLoadCommand
         * @return
         */
        private SpacesResult process( ExpressionExperimentLoadCommand eeLoadCommand ) {
            SpacesExpressionExperimentLoadCommand jsCommand = createCommandObject( eeLoadCommand );
            SpacesResult result = eeTaskProxy.execute( jsCommand );
            return result;
        }

        /**
         * @param model
         * @param list
         * @return
         */
        @Override
        @SuppressWarnings("unchecked")
        protected ModelAndView processGEODataJob( ExpressionExperimentLoadCommand eeLoadCommand ) {
            SpacesResult result = process( eeLoadCommand );
            return super.processGeoLoadResult( ( Collection<ExpressionExperiment> ) result.getAnswer() );
        }

        @Override
        @SuppressWarnings("unchecked")
        protected ModelAndView processPlatformOnlyJob( ExpressionExperimentLoadCommand eeLoadCommand ) {
            SpacesResult result = process( eeLoadCommand );
            return super.processArrayDesignResult( ( Collection<ArrayDesign> ) result.getAnswer() );
        }

        private SpacesExpressionExperimentLoadCommand createCommandObject( ExpressionExperimentLoadCommand eelc ) {
            return new SpacesExpressionExperimentLoadCommand( taskId, eelc.isLoadPlatformOnly(), eelc
                    .isSuppressMatching(), eelc.getAccession(), eelc.isAggressiveQtRemoval(), eelc.isArrayExpress(),
                    eelc.getArrayDesignName() );
        }

    }

    /**
     * Regular job.
     */
    private class LoadJob extends BackgroundControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public LoadJob( String taskId, SecurityContext parentSecurityContext, Object commandObj, MessageUtil messenger ) {
            super( taskId, parentSecurityContext, commandObj, messenger );
            if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            SecurityContextHolder.setContext( securityContext );

            ExpressionExperimentLoadCommand expressionExperimentLoadCommand = ( ( ExpressionExperimentLoadCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + expressionExperimentLoadCommand.getAccession() );

            if ( expressionExperimentLoadCommand.isLoadPlatformOnly() ) {
                return processPlatformOnlyJob( expressionExperimentLoadCommand );
            } else if ( expressionExperimentLoadCommand.isArrayExpress() ) {
                return processArrayExpressJob( expressionExperimentLoadCommand );
            } else /* GEO */{
                return processGEODataJob( expressionExperimentLoadCommand );
            }
        }

        /**
         * @param arrayDesigns
         * @return
         */
        protected ModelAndView processArrayDesignResult( Collection<ArrayDesign> arrayDesigns ) {
            Map<Object, Object> model = new HashMap<Object, Object>();
            this.saveMessage( "Successfully loaded " + arrayDesigns.size() + " array designs" );
            model.put( "arrayDesigns", arrayDesigns );

            if ( arrayDesigns.size() == 1 ) {
                return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id="
                        + arrayDesigns.iterator().next().getId() ) );
            }
            String list = "";
            for ( ArrayDesign ad : arrayDesigns )
                list += ad.getId() + ",";
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) );
        }

        /**
         * @param expressionExperimentLoadCommand
         * @param accesionNum
         * @param model
         * @return
         * @throws IOException
         */
        protected ModelAndView processArrayExpressJob( ExpressionExperimentLoadCommand expressionExperimentLoadCommand ) {

            String accession = getAccession( expressionExperimentLoadCommand );
            ExpressionExperiment result = arrayExpressLoadService.load( accession, expressionExperimentLoadCommand
                    .getArrayDesignName() );

            return processArrayExpressResult( result );
        }

        /**
         * @param result
         * @return
         */
        protected ModelAndView processArrayExpressResult( ExpressionExperiment result ) {
            this.saveMessage( "Successfully loaded " + result.getName() );
            Map<Object, Object> model = new HashMap<Object, Object>();
            model.put( "expressionExperiment", result );
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + result.getId() ) );
        }

        /**
         * @param accesionNum
         * @param doSampleMatching
         * @param aggressiveQtRemoval
         * @return
         */
        @SuppressWarnings("unchecked")
        protected ModelAndView processGEODataJob( ExpressionExperimentLoadCommand expressionExperimentLoadCommand ) {

            String accession = getAccession( expressionExperimentLoadCommand );
            boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
            boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();

            Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accession, false,
                    doSampleMatching, aggressiveQtRemoval );

            return processGeoLoadResult( result );
        }

        /**
         * @param result
         * @return
         */
        protected ModelAndView processGeoLoadResult( Collection<ExpressionExperiment> result ) {
            Map<Object, Object> model = new HashMap<Object, Object>();
            if ( result.size() == 1 ) {
                ExpressionExperiment loaded = result.iterator().next();
                this.saveMessage( "Successfully loaded " + loaded );
                model.put( "expressionExperiment", loaded );
                return new ModelAndView( new RedirectView(
                        "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                + result.iterator().next().getId() ) );
            }

            this.saveMessage( "Successfully loaded " + result.size() + " expression experiments" );
            String list = "";
            for ( ExpressionExperiment ee : result )
                list += ee.getId() + ",";
            return new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperiments.html?ids=" + list ) );
        }

        /**
         * @param job
         * @param accesionNum
         * @param doSampleMatching
         * @param aggressiveQtRemoval
         * @param model
         * @param list
         * @return
         */
        @SuppressWarnings("unchecked")
        protected ModelAndView processPlatformOnlyJob( ExpressionExperimentLoadCommand expressionExperimentLoadCommand ) {
            String accession = getAccession( expressionExperimentLoadCommand );

            boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
            boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();

            Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accession, true, doSampleMatching,
                    aggressiveQtRemoval );

            return processArrayDesignResult( arrayDesigns );
        }

        /**
         * Clean up the access provided by the user.
         * 
         * @param expressionExperimentLoadCommand
         * @return
         */
        private String getAccession( ExpressionExperimentLoadCommand expressionExperimentLoadCommand ) {
            String accesionNum = expressionExperimentLoadCommand.getAccession();
            accesionNum = StringUtils.strip( accesionNum );
            accesionNum = StringUtils.upperCase( accesionNum );
            return accesionNum;
        }
    }

}
