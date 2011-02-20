/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
import ubic.gemma.tasks.analysis.expression.BatchInfoFetchTask;
import ubic.gemma.tasks.maintenance.ExpressionExperimentReportTaskCommand;

/**
 * For populating "batch" information about experiments.
 * 
 * @author paul
 * @version $Id$
 */
@Controller
public class BatchInfoFetchController extends AbstractTaskService {

    private class BatchInfoFetchJob extends BackgroundJob<ExpressionExperimentReportTaskCommand> {

        /**
         * @param taskId
         * @param commandObj
         */
        public BatchInfoFetchJob( ExpressionExperimentReportTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return batchInfoFetchTask.execute( command );
        }

    }

    /**
     * Job that loads in a javaspace.
     */
    private class BatchInfoFetchSpaceJob extends BatchInfoFetchJob {

        final BatchInfoFetchTask taskProxy = ( BatchInfoFetchTask ) getProxy();

        public BatchInfoFetchSpaceJob( ExpressionExperimentReportTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }

    }

    @Autowired
    private BatchInfoFetchTask batchInfoFetchTask;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    public BatchInfoFetchController() {
        super();
        this.setBusinessInterface( BatchInfoFetchTask.class );
    }

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        if ( id == null ) throw new IllegalArgumentException( "ID cannot be null" );

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) throw new IllegalArgumentException( "Could not load experiment with id=" + id );
        ee = expressionExperimentService.thawLite( ee );
        ExpressionExperimentReportTaskCommand cmd = new ExpressionExperimentReportTaskCommand( ee );

        return super.run( cmd );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getInProcessRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return new BatchInfoFetchJob( ( ExpressionExperimentReportTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getSpaceRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return new BatchInfoFetchSpaceJob( ( ExpressionExperimentReportTaskCommand ) command );

    }

}
