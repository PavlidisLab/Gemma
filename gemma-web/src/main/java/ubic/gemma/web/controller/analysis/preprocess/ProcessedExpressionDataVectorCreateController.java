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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.tasks.analysis.expression.ProcessedExpressionDataVectorCreateTask;
import ubic.gemma.tasks.analysis.expression.ProcessedExpressionDataVectorCreateTaskCommand;

/**
 * A controller to preprocess expression data vectors.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class ProcessedExpressionDataVectorCreateController extends AbstractTaskService {

    public ProcessedExpressionDataVectorCreateController() {
        super();
        this.setBusinessInterface( ProcessedExpressionDataVectorCreateTask.class );
    }

    @Autowired
    ProcessedExpressionDataVectorCreateTask processedExpressionDataVectorCreateTask;

    /**
     * @author keshav
     */
    private class ProcessedExpressionDataVectorCreateJob extends
            BackgroundJob<ProcessedExpressionDataVectorCreateTaskCommand> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ProcessedExpressionDataVectorCreateJob( ProcessedExpressionDataVectorCreateTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return processedExpressionDataVectorCreateTask.execute( command );
        }

    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class ProcessedExpressionDataVectorCreateSpaceJob extends ProcessedExpressionDataVectorCreateJob {

        final ProcessedExpressionDataVectorCreateTask taskProxy = ( ProcessedExpressionDataVectorCreateTask ) getProxy();

        public ProcessedExpressionDataVectorCreateSpaceJob( ProcessedExpressionDataVectorCreateTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }

    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {

        ExpressionExperiment ee = expressionExperimentService.load( id );
        ee = expressionExperimentService.thawLite( ee );
        ProcessedExpressionDataVectorCreateTaskCommand cmd = new ProcessedExpressionDataVectorCreateTaskCommand( ee );

        return super.run( cmd );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ProcessedExpressionDataVectorCreateTaskCommand> getInProcessRunner( TaskCommand command ) {
        return new ProcessedExpressionDataVectorCreateJob( ( ProcessedExpressionDataVectorCreateTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ProcessedExpressionDataVectorCreateTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new ProcessedExpressionDataVectorCreateSpaceJob(
                ( ProcessedExpressionDataVectorCreateTaskCommand ) command );
    }

}
