/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.job;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.job.progress.ProgressUpdateContext;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

/**
 * Task Lifecycle Hooks ProgressUpdateAppender -
 *
 * @author anton
 */
@CommonsLog
class ExecutingTask implements Callable<TaskResult> {

    private final Task<?> task;

    @Nullable
    private TaskLifecycleHandler lifecycleHandler;

    public ExecutingTask( Task<?> task ) {
        this.task = task;
    }

    @Override
    public final TaskResult call() {
        TaskResult result;

        runPhase( TaskPhase.START, null );

        try ( ProgressUpdateContext ignored = ProgressUpdateContext.createContext( this::progressUpdate ) ) {
            // From here we are running as user who submitted the task.
            result = this.task.call();
        } catch ( Exception e ) {
            // result is an exception
            result = new TaskResult( task.getTaskCommand().getTaskId(), e );
        }

        if ( result.getException() == null ) {
            runPhase( TaskPhase.SUCCESS, null );
        } else {
            runPhase( TaskPhase.FAILURE, result.getException() );
        }

        runPhase( TaskPhase.COMPLETE, null );

        return result;
    }

    public void setLifecycleHandler( @Nullable TaskLifecycleHandler lifecycleHandler ) {
        this.lifecycleHandler = lifecycleHandler;
    }

    private void progressUpdate( String message ) {
        runPhase( TaskPhase.PROGRESS, message );
    }

    private void runPhase( TaskPhase phase, @Nullable Object arg ) {
        if ( lifecycleHandler == null ) {
            return;
        }
        try {
            switch ( phase ) {
                case START:
                    lifecycleHandler.onStart();
                    break;
                case PROGRESS:
                    lifecycleHandler.onProgress( ( String ) requireNonNull( arg ) );
                    break;
                case SUCCESS:
                    lifecycleHandler.onSuccess();
                case FAILURE:
                    lifecycleHandler.onFailure( ( Exception ) requireNonNull( arg ) );
                    break;
                case COMPLETE:
                    lifecycleHandler.onComplete();
                    break;
            }
        } catch ( Exception e ) {
            log.error( String.format( "An error occurred while running lifecycle phase %s for task with ID %s.",
                    phase, task.getTaskCommand().getTaskId() ), e );
        }
    }
}