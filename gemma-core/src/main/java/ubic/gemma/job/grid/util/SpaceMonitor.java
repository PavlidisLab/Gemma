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
package ubic.gemma.job.grid.util;

import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.progress.ProgressData;
import ubic.gemma.job.progress.ProgressStatusService;
import ubic.gemma.util.ConfigUtils;

/**
 * Wire to the scheduler to periodically run a job that helps monitor the space. If the job runs successfully on the
 * grid getLastStatusWasOK() will return true.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class SpaceMonitor extends AbstractTaskService {

    private final static long TIMEOUT = 10000;

    private Boolean enabled = true;

    private String lastStatusMessage = "";

    private Boolean lastStatusWasOK = true;

    @Autowired
    private ProgressStatusService progressStatusService;

    public SpaceMonitor() {
        this.setBusinessInterface( MonitorTask.class );
    }

    public void disable() {
        this.enabled = false;
    }

    /**
     * @return the lastStatusMessage
     */
    public String getLastStatusMessage() {
        return lastStatusMessage;
    }

    /**
     * @return the lastStatusWasOK
     */
    public Boolean getLastStatusWasOK() {
        return lastStatusWasOK;
    }

    /**
     * This will be fired by quartz. Sends notifications if the space isn't functioning as expected.
     * 
     * @return true if everything is nominal. Note that this return value doesn't really do anything when triggered by
     *         quartz.
     */
    public boolean ping() { 
        if ( !enabled ) {
            this.lastStatusMessage = "";
            this.lastStatusWasOK = true;
            return true;
        }

        boolean allIsWell = true;
        String status = "";

        if ( !ConfigUtils.isGridEnabled() ) {
            // This is okay. We're not expecting the grid.
            status = "Space is not configured";
        } else if ( !SpacesUtil.isSpaceRunning() ) {
            status = "Space is not running, but it is supposed to be.";
            allIsWell = false;
        } else {

            /*
             * Start the task.
             */
            String taskId;
            try {
                taskId = this.run( new MonitorTaskCommand() );
                status = " ----  Last submitted monitor task: " + taskId;
            } catch ( TaskNotGridEnabledException e ) {
                this.lastStatusMessage = e.getMessage();
                this.lastStatusWasOK = false;
                return false;
            }

            /*
             * Wait for the result.
             */
            StopWatch timer = new StopWatch();
            timer.start();
            wait: while ( true ) {
                List<ProgressData> progressStatus = progressStatusService.getProgressStatus( taskId );
                for ( ProgressData progressData : progressStatus ) {
                    if ( progressData.isDone() || progressData.isFailed() ) {
                        try {
                            TaskResult result = ( TaskResult ) taskRunningService.checkResult( taskId );
                            if ( result == null || !result.getRanInSpace() ) {
                                status = "Task " + taskId + " returned bad status - space may not be accepting jobs: "
                                        + progressData.getDescription();
                                allIsWell = false;
                            }

                        } catch ( Exception e ) {
                            status = ExceptionUtils.getStackTrace( e );
                            allIsWell = false;
                        }
                        break wait;
                    }
                }

                try {
                    Thread.sleep( 200 );
                } catch ( InterruptedException e ) {
                    taskRunningService.cancelTask( taskId );
                    status = "Waiting for result was interrupted: " + taskId;
                    allIsWell = false;
                }

                if ( timer.getTime() > TIMEOUT ) {
                    allIsWell = false;
                    status = "Timed out: " + taskId;
                    taskRunningService.cancelTask( taskId );
                    break;
                }
            }
        }

        this.lastStatusMessage = status;
        this.lastStatusWasOK = allIsWell;
        return allIsWell;
    }

    @Override
    protected BackgroundJob<TaskCommand> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<MonitorTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new BackgroundJob<MonitorTaskCommand>( ( MonitorTaskCommand ) command ) {

            @Override
            public TaskResult processJob() {
                MonitorTask task = ( MonitorTask ) getProxy();
                this.getCommand().setPersistJobDetails( false );
                return task.execute( this.getCommand() ); // makes the RMI call
            }
        };
    }
}
