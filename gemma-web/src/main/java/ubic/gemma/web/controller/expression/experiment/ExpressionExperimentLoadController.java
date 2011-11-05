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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.tasks.analysis.expression.ExpressionExperimentLoadTask;
import ubic.gemma.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;

/**
 * Handles loading of Expression data into the system when the source is GEO or ArrayExpress, via Spring MVC or AJAX,
 * either in the webapp or in a javaspaces grid. The choice depends on how the system and client is configured. In
 * either case the job runs in its own thread, firing a ProgressJob that the client can monitor.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.web.controller.expression.experiment.ExpressionDataFileUploadController for how flat-file data is
 *      loaded.
 */
@Controller
public class ExpressionExperimentLoadController extends AbstractTaskService {

    public ExpressionExperimentLoadController() {
        super();
        this.setBusinessInterface( ExpressionExperimentLoadTask.class );
    }

    @RequestMapping("/admin/loadExpressionExperiment.html")
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "/admin/loadExpressionExperimentForm" );
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author Paul
     * @version $Id$
     */
    private class LoadInSpaceJob extends LoadJob {

        final ExpressionExperimentLoadTask eeTaskProxy = ( ExpressionExperimentLoadTask ) getProxy();

        /**
         * @param commandObj
         */
        public LoadInSpaceJob( ExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );

        }

        @Override
        protected TaskResult processArrayExpressJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
            TaskResult result = this.process( eeLoadCommand );
            return super.processArrayExpressResult( ( ExpressionExperiment ) result.getAnswer() );
        }

        /**
         * @param model
         * @param list
         * @return
         */
        @Override
        protected TaskResult processGEODataJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
            TaskResult result = this.process( eeLoadCommand );
            return super.processGeoLoadResult( ( Collection<ExpressionExperiment> ) result.getAnswer() );
        }

        @Override
        protected TaskResult processPlatformOnlyJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
            TaskResult result = this.process( eeLoadCommand );
            return super.processArrayDesignResult( ( Collection<ArrayDesign> ) result.getAnswer() );
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
                throw new RuntimeException( e );
            }
        }

    }

    /**
     * Regular job.
     */
    private class LoadJob extends BackgroundJob<ExpressionExperimentLoadTaskCommand> {

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public LoadJob( ExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
            if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
                geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public TaskResult processJob() {

            if ( command.isLoadPlatformOnly() ) {
                return processPlatformOnlyJob( command );
            } else if ( command.isArrayExpress() ) {
                return processArrayExpressJob( command );
            } else /* GEO */{
                return processGEODataJob( command );
            }
        }

        /**
         * @param arrayDesigns
         * @return
         */
        protected TaskResult processArrayDesignResult( Collection<ArrayDesign> arrayDesigns ) {
            Map<Object, Object> model = new HashMap<Object, Object>();
            model.put( "arrayDesigns", arrayDesigns );

            if ( arrayDesigns.size() == 1 ) {
                return new TaskResult( command, new ModelAndView( new RedirectView(
                        "/Gemma/arrays/showArrayDesign.html?id=" + arrayDesigns.iterator().next().getId() ) ) );
            }
            String list = "";
            for ( ArrayDesign ad : arrayDesigns )
                list += ad.getId() + ",";
            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) ) );
        }

        /**
         * @param expressionExperimentLoadCommand
         * @param accesionNum
         * @param model
         * @return
         * @throws IOException
         */
        protected TaskResult processArrayExpressJob( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {

            String accession = getAccession( expressionExperimentLoadCommand );
            ExpressionExperiment result = arrayExpressLoadService.load( accession,
                    expressionExperimentLoadCommand.getArrayDesignName(),
                    expressionExperimentLoadCommand.isAllowArrayExpressDesign() );

            return processArrayExpressResult( result );
        }

        /**
         * @param result
         * @return
         */
        protected TaskResult processArrayExpressResult( ExpressionExperiment result ) {
            if ( result == null ) {
                throw new IllegalStateException( "Loading failed" );
            }
            Map<Object, Object> model = new HashMap<Object, Object>();
            model.put( "expressionExperiment", result );
            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + result.getId() ) ) );
        }

        /**
         * @param accesionNum
         * @param doSampleMatching
         * @param aggressiveQtRemoval
         * @return
         */
        protected TaskResult processGEODataJob( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {

            String accession = getAccession( expressionExperimentLoadCommand );
            boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
            boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
            boolean splitIncompatiblePlatforms = expressionExperimentLoadCommand.isSplitByPlatform();
            boolean allowSuperSeriesLoad = expressionExperimentLoadCommand.isAllowSuperSeriesLoad();
            boolean allowSubSeriesLoad = true; // FIXME

            Collection<ExpressionExperiment> result = geoDatasetService.fetchAndLoad( accession, false,
                    doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad,
                    allowSubSeriesLoad );

            if ( result == null ) {
                throw new RuntimeException( "No results were returned (cancelled or failed)" );
            }

            postProcess( result );

            return processGeoLoadResult( result );
        }

        /**
         * @param result
         * @return
         */
        protected TaskResult processGeoLoadResult( Collection<ExpressionExperiment> result ) {
            Map<Object, Object> model = new HashMap<Object, Object>();
            if ( result == null || result.size() == 0 ) {
                throw new RuntimeException( "No results were returned (cancelled or failed)" );
            }
            if ( result.size() == 1 ) {
                ExpressionExperiment loaded = result.iterator().next();
                model.put( "expressionExperiment", loaded );
                return new TaskResult( command, new ModelAndView( new RedirectView(
                        "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                                + result.iterator().next().getId() ) ) );
            }

            String list = "";
            for ( ExpressionExperiment ee : result ) {
                list += ee.getId() + ",";
            }
            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperiments.html?ids=" + list ) ) );
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
        protected TaskResult processPlatformOnlyJob( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {
            String accession = getAccession( expressionExperimentLoadCommand );

            boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
            boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
            Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService.fetchAndLoad(
                    accession, true, doSampleMatching, aggressiveQtRemoval, false );

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
                ee = eeService.thawLite( ee );
                twoChannelMissingValueService.computeMissingValues( ee );
                wasProcessed = true;
            }

            return wasProcessed;
        }
    }

    @Autowired
    GeoDatasetService geoDatasetService;

    @Autowired
    ArrayExpressLoadService arrayExpressLoadService;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    TwoChannelMissingValues twoChannelMissingValueService;

    /**
     * Main entry point for AJAX calls.
     * 
     * @param command
     * @return
     */
    public String load( ExpressionExperimentLoadTaskCommand command ) {
        // remove stray whitespace.
        command.setAccession( StringUtils.strip( command.getAccession() ) );

        if ( StringUtils.isBlank( command.getAccession() ) ) {
            throw new IllegalArgumentException( "Must provide an accession" );
        }

        return super.run( command );
    }

    /**
     * @param arrayExpressLoadService the arrayExpressLoadService to set
     */
    public void setArrayExpressLoadService( ArrayExpressLoadService arrayExpressLoadService ) {
        this.arrayExpressLoadService = arrayExpressLoadService;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    /**
     * @param geoDatasetService the geoDatasetService to set
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
    }

    public void setProcessedExpressionDataVectorCreateService(
            ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService ) {
        this.processedExpressionDataVectorCreateService = processedExpressionDataVectorCreateService;
    }

    public void setTwoChannelMissingValueService( TwoChannelMissingValues twoChannelMissingValueService ) {
        this.twoChannelMissingValueService = twoChannelMissingValueService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ExpressionExperimentLoadTaskCommand> getInProcessRunner( TaskCommand command ) {

        return new LoadJob( ( ExpressionExperimentLoadTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ExpressionExperimentLoadTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new LoadInSpaceJob( ( ExpressionExperimentLoadTaskCommand ) command );
    }

}
