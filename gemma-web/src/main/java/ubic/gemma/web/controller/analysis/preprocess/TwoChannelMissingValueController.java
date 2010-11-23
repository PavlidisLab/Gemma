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
import ubic.gemma.tasks.analysis.expression.TwoChannelMissingValueTask;
import ubic.gemma.tasks.analysis.expression.TwoChannelMissingValueTaskCommand;

/**
 * Run misssing value computation via web request.
 * 
 * @author paul
 * @version $Id$
 */
@Controller
public class TwoChannelMissingValueController extends AbstractTaskService {

    public TwoChannelMissingValueController() {
        super();
        this.setBusinessInterface( TwoChannelMissingValueTask.class );
    }

    @Autowired
    TwoChannelMissingValueTask twoChannelMissingValueTask;

    private class TwoChannelMissingValueJob extends BackgroundJob<TwoChannelMissingValueTaskCommand> {

        public TwoChannelMissingValueJob( TwoChannelMissingValueTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            return twoChannelMissingValueTask.execute( this.command );
        }

    }

    private class TwoChannelMissingValueSpaceJob extends BackgroundJob<TwoChannelMissingValueTaskCommand> {

        final TwoChannelMissingValueTask taskProxy = ( TwoChannelMissingValueTask ) getProxy();

        public TwoChannelMissingValueSpaceJob( TwoChannelMissingValueTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            return taskProxy.execute( this.command );
        }

    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    /**
     * AJAX entry point. -- uses default settings
     * 
     * @param id
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( id );

        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }
        ee = expressionExperimentService.thawLite( ee );

        TwoChannelMissingValueTaskCommand cmd = new TwoChannelMissingValueTaskCommand( ee );

        return super.run( cmd );
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    protected BackgroundJob<TwoChannelMissingValueTaskCommand> getInProcessRunner( TaskCommand command ) {
        return new TwoChannelMissingValueJob( ( TwoChannelMissingValueTaskCommand ) command );
    }

    @Override
    protected BackgroundJob<TwoChannelMissingValueTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new TwoChannelMissingValueSpaceJob( ( TwoChannelMissingValueTaskCommand ) command );
    }

}
