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
package ubic.gemma.web.controller.analysis.preprocess;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.analysis.preprocess.ProcessedExpressionDataVectorCreateTask;
import ubic.gemma.grid.javaspaces.analysis.preprocess.SpacesProcessedExpressionDataVectorCreateCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseCommand;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to preprocess expression data vectors.
 * 
 * @spring.bean id="processedExpressionDataVectorCreateController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @author keshav
 * @version $Id$
 */
public class ProcessedExpressionDataVectorCreateController extends AbstractSpacesController {

    private ExpressionExperimentService expressionExperimentService = null;

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

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
        expressionExperimentService.thaw( ee );

        ProcessedExpressionDataVectorCreateCommand cmd = new ProcessedExpressionDataVectorCreateCommand();
        cmd.setExpressionExperiment( ee );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ProcessedExpressionDataVectorCreateTask.class
                .getName(), true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob getRunner( String jobId, Object command ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob getSpaceRunner( String jobId, Object command ) {
        return new ProcessedExpressionDataVectorCreateSpaceJob( jobId, command );
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class ProcessedExpressionDataVectorCreateSpaceJob extends ProcessedExpressionDataVectorCreateJob {

        final ProcessedExpressionDataVectorCreateTask taskProxy = ( ProcessedExpressionDataVectorCreateTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public ProcessedExpressionDataVectorCreateSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.web.controller.analysis.preprocess.ProcessedExpressionDataVectorCreateController.ProcessedExpressionDataVectorCreateJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand baseCommand ) {
            ProcessedExpressionDataVectorCreateCommand vectorCommand = ( ProcessedExpressionDataVectorCreateCommand ) baseCommand;
            process( vectorCommand );
            return new ModelAndView( new RedirectView( "/Gemma" ) );
        }

        /**
         * @param diffCommand
         * @return
         */
        private SpacesResult process( ProcessedExpressionDataVectorCreateCommand command ) {
            SpacesProcessedExpressionDataVectorCreateCommand jsCommand = createCommandObject( command );
            SpacesResult result = taskProxy.execute( jsCommand );
            return result;
        }

        /**
         * @param command
         * @return
         */
        protected SpacesProcessedExpressionDataVectorCreateCommand createCommandObject(
                ProcessedExpressionDataVectorCreateCommand command ) {
            return new SpacesProcessedExpressionDataVectorCreateCommand( taskId, command.getExpressionExperiment() );
        }

    }

    /**
     * @author keshav
     */
    private class ProcessedExpressionDataVectorCreateJob extends BaseControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ProcessedExpressionDataVectorCreateJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            ProcessedExpressionDataVectorCreateCommand vectorCommand = ( ( ProcessedExpressionDataVectorCreateCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + vectorCommand.getExpressionExperiment().getShortName() );

            return processJob( vectorCommand );
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
        // TODO Auto-generated method stub
        return null;
    }

}
