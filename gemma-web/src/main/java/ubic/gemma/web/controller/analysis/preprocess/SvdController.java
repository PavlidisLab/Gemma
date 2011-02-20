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
import ubic.gemma.tasks.analysis.expression.SvdTask;
import ubic.gemma.tasks.maintenance.SvdTaskCommand;

/**
 * Run SVD on a data set.
 * 
 * @author paul
 * @version $Id$
 */
@Controller
public class SvdController extends AbstractTaskService {
    private class SvdJob extends BackgroundJob<SvdTaskCommand> {

        /**
         * @param taskId
         * @param commandObj
         */
        public SvdJob( SvdTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return svdTask.execute( command );
        }

    }

    /**
     * Job that loads in a javaspace.
     */
    private class SvdSpaceJob extends SvdJob {

        final SvdTask taskProxy = ( SvdTask ) getProxy();

        public SvdSpaceJob( SvdTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }

    }

    @Autowired
    private SvdTask svdTask;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    public SvdController() {
        super();
        this.setBusinessInterface( SvdTask.class );
    }

    /**
     * AJAX entry point.
     * 
     * @param id
     * @param postprocessOnly if we should not run the SVD, just the comparison to factors
     * @return
     * @throws Exception
     */
    public String run( Long id, boolean postprocessOnly ) throws Exception {
        if ( id == null ) throw new IllegalArgumentException( "ID cannot be null" );
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) throw new IllegalArgumentException( "Could not load experiment with id=" + id );

        ee = expressionExperimentService.thawLite( ee );
        SvdTaskCommand cmd = new SvdTaskCommand( ee, postprocessOnly );

        return super.run( cmd );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getInProcessRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return new SvdJob( ( SvdTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getSpaceRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return new SvdSpaceJob( ( SvdTaskCommand ) command );

    }

}
