/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.web.controller.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.grid.util.MonitorTask;
import ubic.gemma.job.grid.util.MonitorTaskCommand;

/**
 * Exists entirely to create a dumb job that doesn't do anything, for test purposes.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class TestTaskController extends AbstractTaskService {

    @Autowired
    private MonitorTask testTask;

    public TestTaskController() {
        this.setBusinessInterface( MonitorTask.class );
    }

    /**
     * AJAX method
     * 
     * @param runtime How long the task should take
     * @param forceLocal Whether to force it to run inprocess vs. the grid (if the latter is available)
     * @param fail Should it fail (it will ignore runtime parameter)
     * @param persistDetails Should the jobDetails be saved in the database. Leave this false if you are running on a
     *        production system to avoid cluttering up the db.
     * @return taskId
     */
    public String run( int runtime, boolean forceLocal, boolean fail, boolean persistDetails ) {

        MonitorTaskCommand command = new MonitorTaskCommand();

        command.setRunTimeMillis( runtime );
        command.setPersistJobDetails( persistDetails );
        command.setFail( fail );

        if ( forceLocal ) {
            return this.startTask( getInProcessRunner( command ) );
        }
        return super.run( command );
    }

    @Override
    protected BackgroundJob<MonitorTaskCommand> getInProcessRunner( TaskCommand command ) {
        return new BackgroundJob<MonitorTaskCommand>( ( MonitorTaskCommand ) command ) {
            @Override
            public TaskResult processJob() {
                return testTask.execute( this.getCommand() );
            }
        };
    }

    @Override
    protected BackgroundJob<MonitorTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new BackgroundJob<MonitorTaskCommand>( ( MonitorTaskCommand ) command ) {
            @Override
            public TaskResult processJob() {
                MonitorTask task = ( MonitorTask ) getProxy();
                return task.execute( this.getCommand() ); // makes the RMI call
            }
        };
    }

}
