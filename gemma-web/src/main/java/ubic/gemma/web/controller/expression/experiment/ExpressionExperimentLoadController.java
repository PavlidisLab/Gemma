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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentLoadTask;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentLoadTaskCommand;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * Handles loading of Expression data into the system when the source is GEO or ArrayExpress, via Spring MVC or AJAX,
 * either in the webapp or in a javaspaces grid. The choice depends on how the system and client is configured. In
 * either case the job runs in its own thread, firing a ProgressJob that the client can monitor.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentLoadController"
 * @spring.property name="geoDatasetService" ref="geoDatasetService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="arrayExpressLoadService" ref="arrayExpressLoadService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="twoChannelMissingValueService" ref="twoChannelMissingValues"
 * @spring.property name="processedExpressionDataVectorCreateService" ref="processedExpressionDataVectorCreateService"
 * @see ubic.gemma.web.controller.expression.experiment.ExpressionDataFileUploadController for how flat-file data is
 *      loaded.
 */
public class ExpressionExperimentLoadController extends AbstractSpacesController<ModelAndView> {

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    GeoDatasetService geoDatasetService;

    ArrayDesignService arrayDesignService;

    ArrayExpressLoadService arrayExpressLoadService;
    ExpressionExperimentService eeService;
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;
    TwoChannelMissingValues twoChannelMissingValueService;

    public void setProcessedExpressionDataVectorCreateService(
            ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService ) {
        this.processedExpressionDataVectorCreateService = processedExpressionDataVectorCreateService;
    }

    public void setTwoChannelMissingValueService( TwoChannelMissingValues twoChannelMissingValueService ) {
        this.twoChannelMissingValueService = twoChannelMissingValueService;
    }

    /**
     * Main entry point for AJAX calls.
     * 
     * @param command
     * @return
     */
    public String run( ExpressionExperimentLoadTaskCommand command ) {
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
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, Object command ) {

        return new LoadJob( taskId, command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String taskId, Object command ) {
        return new LoadInSpaceJob( taskId, command );
    }

    /**
     * This method has been deprecated in favor of an ajax call.
     * 
     * @param request
     * @deprecated
     */
    @Deprecated
    private void cancel( HttpServletRequest request ) {
        Future job = ( Future ) request.getSession().getAttribute( TaskRunningService.JOB_ATTRIBUTE );
        job.cancel( true );
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
        public LoadInSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        @Override
        protected ModelAndView processArrayExpressJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
            TaskResult result = this.process( eeLoadCommand );
            return super.processArrayExpressResult( ( ExpressionExperiment ) result.getAnswer() );
        }

        /**
         * @param cmd
         * @return
         */
        private TaskResult process( ExpressionExperimentLoadTaskCommand cmd ) {
            cmd.setTaskId( this.taskId );
            try {
                TaskResult result = eeTaskProxy.execute( cmd );
                return result;
            } catch ( Exception e ) {
                if ( e instanceof InterruptedException ) {
                    log.info( "Job was cancelled" );
                    return null;
                }
                throw new RuntimeException( e );
            }
        }

        /**
         * @param model
         * @param list
         * @return
         */
        @Override
        @SuppressWarnings("unchecked")
        protected ModelAndView processGEODataJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
            TaskResult result = this.process( eeLoadCommand );
            return super.processGeoLoadResult( ( Collection<ExpressionExperiment> ) result.getAnswer() );
        }

        @Override
        @SuppressWarnings("unchecked")
        protected ModelAndView processPlatformOnlyJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
            TaskResult result = this.process( eeLoadCommand );
            return super.processArrayDesignResult( ( Collection<ArrayDesign> ) result.getAnswer() );
        }

    }

    /**
     * Regular job.
     */
    private class LoadJob extends BaseControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public LoadJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
            if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            }

        }

        /*
         * (non-Javadoc)
         * @see java.util.concurrent.Callable#call()
         */
        public ModelAndView call() throws Exception {

            ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand = ( ( ExpressionExperimentLoadTaskCommand ) command );

            super.initializeProgressJob( expressionExperimentLoadCommand.getAccession() );

            return processJob( expressionExperimentLoadCommand );

        }

        /*
         * (non-Javadoc)
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.grid.javaspaces.TaskCommand)
         */
        @Override
        protected ModelAndView processJob( TaskCommand c ) {

            ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand = ( ExpressionExperimentLoadTaskCommand ) c;

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
        protected ModelAndView processArrayExpressJob(
                ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {

            String accession = getAccession( expressionExperimentLoadCommand );
            ExpressionExperiment result = arrayExpressLoadService.load( accession, expressionExperimentLoadCommand
                    .getArrayDesignName(), expressionExperimentLoadCommand.isAllowArrayExpressDesign() );

            return processArrayExpressResult( result );
        }

        /**
         * @param result
         * @return
         */
        protected ModelAndView processArrayExpressResult( ExpressionExperiment result ) {
            if ( result == null ) {
                throw new IllegalStateException( "Loading failed" );
            }
            this.saveMessage( "Successfully loaded " + result );
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
        protected ModelAndView processGEODataJob( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {

            String accession = getAccession( expressionExperimentLoadCommand );
            boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
            boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
            boolean splitIncompatiblePlatforms = expressionExperimentLoadCommand.isSplitByPlatform();
            boolean allowSuperSeriesLoad = expressionExperimentLoadCommand.isAllowSuperSeriesLoad();

            Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accession, false,
                    doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad );

            if ( result == null ) {
                return processGeoLoadResult( result );
            }

            postProcess( result );

            return processGeoLoadResult( result );
        }

        /**
         * Do missing value and processed vector creation steps.
         * 
         * @param ees
         */
        private void postProcess( Collection<ExpressionExperiment> ees ) {

            if ( ees == null ) return;

            log.info( "Postprocessing ..." );
            for ( ExpressionExperiment ee : ees ) {

                Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( ee );
                if ( arrayDesignsUsed.size() > 1 ) {
                    log.warn( "Skipping postprocessing because experiment uses "
                            + "multiple array types. Please check valid entry and run postprocessing separately." );
                }

                ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
                processForMissingValues( ee, arrayDesignUsed );
                processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
            }
        }

        /**
         * @param ee
         * @return
         */
        private boolean processForMissingValues( ExpressionExperiment ee, ArrayDesign design ) {

            boolean wasProcessed = false;

            TechnologyType tt = design.getTechnologyType();
            if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
                log.info( ee + " uses a two-color array design, processing for missing values ..." );
                eeService.thawLite( ee );
                twoChannelMissingValueService.computeMissingValues( ee );
                wasProcessed = true;
            }

            return wasProcessed;
        }

        /**
         * @param result
         * @return
         */
        protected ModelAndView processGeoLoadResult( Collection<ExpressionExperiment> result ) {
            Map<Object, Object> model = new HashMap<Object, Object>();
            if ( result == null || result.size() == 0 ) {
                throw new RuntimeException( "No results were returned (cancelled or failed)" );
            }
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
         * For when we're only loading the platform.
         * 
         * @param job
         * @param accesionNum
         * @param doSampleMatching
         * @param aggressiveQtRemoval
         * @param model
         * @param list
         * @return
         */
        @SuppressWarnings("unchecked")
        protected ModelAndView processPlatformOnlyJob(
                ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {
            String accession = getAccession( expressionExperimentLoadCommand );

            boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
            boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
            Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accession, true, doSampleMatching,
                    aggressiveQtRemoval, false, true ); // last parameters are irrelevant.

            return processArrayDesignResult( arrayDesigns );
        }

        /**
         * Clean up the access provided by the user.
         * 
         * @param expressionExperimentLoadCommand
         * @return
         */
        private String getAccession( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {
            String accesionNum = expressionExperimentLoadCommand.getAccession();
            accesionNum = StringUtils.strip( accesionNum );
            accesionNum = StringUtils.upperCase( accesionNum );
            return accesionNum;
        }
    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        return "loadExpressionExperimentForm";
    }

}
