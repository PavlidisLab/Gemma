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
import org.springframework.stereotype.Component;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.ConflictingTaskException;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.progress.ProgressData;
import ubic.gemma.job.progress.ProgressStatusService;
import ubic.gemma.util.ConfigUtils;

/**
 * Wire to the scheduler to periodically run a job that helps monitor the space, and also recover if the space is
 * restarted. If the job runs successfully on the grid getLastStatusWasOK() will return true.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class SpaceMonitor extends AbstractTaskService {

    private final static long TIMEOUT_MILLIS = 15000;

    private Boolean enabled = true;

    private String lastStatusMessage = "Space monitor has not been run yet.";

    private Boolean lastStatusWasOK = true;

    private Integer numberOfPings = 0;

    private Integer numberOfBadPings = 0;

    @Autowired
    private SpacesUtil spacesUtil;

    @Autowired
    private ProgressStatusService progressStatusService;

    public SpaceMonitor() {
        this.setBusinessInterface( MonitorTask.class );
    }

    public void disable() {
        this.enabled = false;
    }

    @Override
    protected BackgroundJob<TaskCommand> getInProcessRunner( TaskCommand command ) {
        return null;
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
     * @return how many times ping has run so far.
     */
    public Integer getNumberOfPings() {
        return numberOfPings;
    }

    public Integer getNumberOfBadPings() {
        return numberOfBadPings;
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

    /**
     * This will be fired by quartz. Sends notifications if the space isn't functioning as expected.
     * 
     * @return true if everything is nominal. Note that this return value doesn't really do anything when triggered by
     *         quartz.
     */
    public boolean ping() {
        if ( !enabled ) {
            this.lastStatusMessage = "Monitor is disabled.";
            this.lastStatusWasOK = true;
            return true;
        }

        boolean allIsWell = true;
        String status = "";

        if ( !ConfigUtils.isGridEnabled() ) {
            // This is okay. We're not expecting the grid.
            status = "Space is not configured";
        } else if ( !SpacesUtilImpl.isSpaceRunning() ) {
            status = "Space is not running, but it is supposed to be.";
            allIsWell = false;
        } else {

            /*
             * Start the task.
             */
            String taskId = null;
            try {
                MonitorTaskCommand command = new MonitorTaskCommand();
                command.setMaxQueueMinutes( ( ( Long ) Math.max( TIMEOUT_MILLIS / 60000, 1 ) ).intValue() );
                taskId = this.run( command );
                status = " ----  Last submitted monitor task: " + taskId;
            } catch ( TaskNotGridEnabledException e ) {
                this.lastStatusMessage = e.getMessage();
                this.lastStatusWasOK = false;
                numberOfBadPings++;
                numberOfPings++;
                log.warn( taskId + " got " + e.getMessage() );
                return false;
            } catch ( ConflictingTaskException e ) {
                this.lastStatusMessage = e.getMessage() + " -- attempting to cancel the old task";
                this.lastStatusWasOK = false;
                taskRunningService.cancelTask( e.getCollidingCommand().getTaskId() );
                numberOfBadPings++;
                numberOfPings++;
                log.warn( taskId + " got " + e.getMessage() );
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
                            TaskResult result = taskRunningService.checkResult( taskId );
                            if ( result == null || !result.getRanInSpace() ) {
                                status = "Task " + taskId + " returned bad status - space may not be accepting jobs: "
                                        + progressData.getDescription();
                                allIsWell = false;
                                log.warn( taskId + " got " + status );                                
                            }

                        } catch ( Exception e ) {
                            status = ExceptionUtils.getStackTrace( e );
                            allIsWell = false;
                            log.warn( taskId + " got " + status );
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
                    log.warn( taskId + " " + status + e.getMessage() );
                }

                if ( timer.getTime() > TIMEOUT_MILLIS ) {
                    allIsWell = false;
                    status = "Timed out: " + taskId;
                    log.warn( taskId + " timed out, cancelling it just in case." );
                    taskRunningService.cancelTask( taskId );
                    break;
                }
            }
        }

        this.lastStatusMessage = status;
        this.lastStatusWasOK = allIsWell;

        if ( !allIsWell ) {
            /*
             * Perhaps we just need to refresh our connection.
             */
            numberOfBadPings++;
            spacesUtil.forceRefreshSpaceBeans();
        }

        numberOfPings++;

        return allIsWell;
    }
}
