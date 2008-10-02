/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.diff;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTask;
import ubic.gemma.grid.javaspaces.diff.SpacesDifferentialExpressionAnalysisCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseCommand;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to run differential expression analysis either locally or in a space.
 * 
 * @spring.bean id="differentialExpressionAnalysisController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisController extends AbstractSpacesController {

    private ExpressionExperimentService expressionExperimentService = null;

    // /**
    // * AJAX entry point.
    // *
    // * @param cmd
    // * @return
    // * @throws Exception
    // */
    // public String run( DiffExpressionAnalysisCommand cmd ) throws Exception {
    // /* this 'run' method is exported in the spring-beans.xml */
    //
    // return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), DifferentialExpressionAnalysisTask.class
    // .getName(), true );
    // }

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        /* this 'run' method is exported in the spring-beans.xml */

        ExpressionExperiment ee = expressionExperimentService.load( id );
        String shortName = ee.getShortName();

        // FIXME jut pass in the ee to the command object so you don't have to get
        // it later.
        DiffExpressionAnalysisCommand cmd = new DiffExpressionAnalysisCommand();
        cmd.setAccession( shortName );
        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), DifferentialExpressionAnalysisTask.class
                .getName(), true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, Object command ) {
        return new DiffAnalysisJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, Object command ) {
        return new DiffAnalysisSpaceJob( jobId, command );

    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class DiffAnalysisSpaceJob extends DiffAnalysisJob {

        final DifferentialExpressionAnalysisTask taskProxy = ( DifferentialExpressionAnalysisTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.web.controller.diff.DifferentialExpressionAnalysisController.DiffAnalysisJob#processJob(ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand baseCommand ) {
            DiffExpressionAnalysisCommand diffCommand = ( DiffExpressionAnalysisCommand ) baseCommand;
            process( diffCommand );
            return new ModelAndView( new RedirectView( "/Gemma" ) );
        }

        /**
         * @param diffCommand
         * @return
         */
        private SpacesResult process( DiffExpressionAnalysisCommand diffCommand ) {
            SpacesDifferentialExpressionAnalysisCommand jsCommand = createCommandObject( diffCommand );
            SpacesResult result = taskProxy.execute( jsCommand );
            return result;
        }

        protected SpacesDifferentialExpressionAnalysisCommand createCommandObject(
                DiffExpressionAnalysisCommand diffCommand ) {
            return new SpacesDifferentialExpressionAnalysisCommand( taskId, diffCommand.isForceAnalysis(), diffCommand
                    .getAccession() );
        }

    }

    /**
     * Regular (local) job.
     */
    private class DiffAnalysisJob extends BaseControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            DiffExpressionAnalysisCommand diffAnalysisCommand = ( ( DiffExpressionAnalysisCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + diffAnalysisCommand.getAccession() );

            return processJob( diffAnalysisCommand );
        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand command ) {
            throw new UnsupportedOperationException( "Cannot run locally at this time.  Run in a space." );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        return "differentialExpressionAnalysis";
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }
}
