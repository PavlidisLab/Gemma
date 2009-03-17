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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.analysis.preprocess.ProcessedExpressionDataVectorCreateTask;
import ubic.gemma.grid.javaspaces.analysis.preprocess.ProcessedExpressionDataVectorCreateTaskCommand;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to preprocess expression data vectors.
 * 
 * @spring.bean id="processedExpressionDataVectorCreateController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="processedExpressionDataVectorCreateService" ref="processedExpressionDataVectorCreateService"
 * @author keshav
 * @version $Id$
 */
public class ProcessedExpressionDataVectorCreateController extends AbstractSpacesController<Boolean> {

    private ExpressionExperimentService expressionExperimentService = null;

    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService = null;

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

        ExpressionExperiment ee = expressionExperimentService.load( id );

        ProcessedExpressionDataVectorCreateTaskCommand cmd = new ProcessedExpressionDataVectorCreateTaskCommand( ee );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ProcessedExpressionDataVectorCreateTask.class
                .getName(), true );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<Boolean> getRunner( String jobId, Object command ) {
        return new ProcessedExpressionDataVectorCreateJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<Boolean> getSpaceRunner( String jobId, Object command ) {
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
         * @seeubic.gemma.web.controller.analysis.preprocess.ProcessedExpressionDataVectorCreateController.
         * ProcessedExpressionDataVectorCreateJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected Boolean processJob( TaskCommand baseCommand ) {
            baseCommand.setTaskId( this.taskId );
            ProcessedExpressionDataVectorCreateTaskCommand vectorCommand = ( ProcessedExpressionDataVectorCreateTaskCommand ) baseCommand;
            process( vectorCommand );
            return true;
        }

        /**
         * @param command
         * @return
         */
        private TaskResult process( ProcessedExpressionDataVectorCreateTaskCommand command ) {
            ProcessedExpressionDataVectorCreateTaskCommand jsCommand = createCommandObject( command );
            TaskResult result = taskProxy.execute( jsCommand );
            return result;
        }

        /**
         * @param command
         * @return
         */
        protected ProcessedExpressionDataVectorCreateTaskCommand createCommandObject(
                ProcessedExpressionDataVectorCreateTaskCommand command ) {
            return new ProcessedExpressionDataVectorCreateTaskCommand( taskId, command.getExpressionExperiment() );
        }

    }

    public void setProcessedExpressionDataVectorCreateService(
            ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService ) {
        this.processedExpressionDataVectorCreateService = processedExpressionDataVectorCreateService;
    }

    /**
     * @author keshav
     */
    private class ProcessedExpressionDataVectorCreateJob extends BaseControllerJob<Boolean> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ProcessedExpressionDataVectorCreateJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        /*
         * (non-Javadoc)
         * @see java.util.concurrent.Callable#call()
         */
        public Boolean call() throws Exception {

            ProcessedExpressionDataVectorCreateTaskCommand vectorCommand = ( ( ProcessedExpressionDataVectorCreateTaskCommand ) command );

            super.initializeProgressJob( vectorCommand.getExpressionExperiment().getShortName() );

            return processJob( vectorCommand );
        }

        /*
         * (non-Javadoc)
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected Boolean processJob( TaskCommand c ) {
            ProcessedExpressionDataVectorCreateTaskCommand vectorCommand = ( ( ProcessedExpressionDataVectorCreateTaskCommand ) c );

            ExpressionExperiment ee = vectorCommand.getExpressionExperiment();
            // expressionExperimentService.thawLite( ee );

            try {
                Collection<ProcessedExpressionDataVector> processedVectors = processedExpressionDataVectorCreateService
                        .computeProcessedExpressionData( ee );
                return processedVectors.size() > 0;
            } catch ( DataIntegrityViolationException e ) {
                log.error( e, e );
                throw new RuntimeException(
                        "The processed vectors could not be created, probably because analysis results refer to the old ones."
                                + " Old analyses must be deleted first. Detailed message: " + e.getMessage() );
            }
        }
    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        return null;
    }

}
