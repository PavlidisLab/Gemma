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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author anton
 */
public class ExecutingTaskTest extends BaseSpringContextTest {

    @Test
    public void testOrderOfExecutionFailure() {
        TaskCommand taskCommand = new TestTaskCommand();
        Task<TaskCommand> task = new FailureTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask executingTask = new ExecutingTask( TaskUtils.generateTaskId(), task );
        TaskLifecycleHandler lifecycleHandler = mock( TaskLifecycleHandler.class );
        executingTask.setLifecycleHandler( lifecycleHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );

        TaskResult taskResult = null;
        try {
            taskResult = future.get();
        } catch ( InterruptedException | ExecutionException e ) {
            e.printStackTrace();
            fail();
        }
        Exception exception = taskResult.getException();
        assertNotNull( exception );
        assertEquals( AccessDeniedException.class, exception.getClass() );
        assertEquals( "Test!", exception.getMessage() );

        verify( lifecycleHandler ).onStart();
        verify( lifecycleHandler ).onProgress( "Executing a failing task." );
        verify( lifecycleHandler ).onFailure( any( Exception.class ) );
        verify( lifecycleHandler ).onComplete();
    }

    // TODO: Test security context under different failure scenarios
    // - exception in the task
    // - exception in the lifecycle hook
    // - exception in progress appender setup/teardown hook
    @Test
    public void testOrderOfExecutionSuccess() {
        TaskCommand taskCommand = new TestTaskCommand();
        Task<TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask executingTask = new ExecutingTask( TaskUtils.generateTaskId(), task );
        TaskLifecycleHandler lifecycleHandler = mock( TaskLifecycleHandler.class );
        executingTask.setLifecycleHandler( lifecycleHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        this.tryGetAnswer( future );

        verify( lifecycleHandler ).onStart();
        verify( lifecycleHandler ).onProgress( "Executing a success task." );
        verify( lifecycleHandler ).onSuccess();
        verify( lifecycleHandler ).onComplete();
    }

    @Test
    public void testSecurityContextManagement() {

        this.runAsUser( "ExecutingTaskTestUser" );
        TaskCommand taskCommand = new TestTaskCommand();
        Authentication executingUserAuth = SecurityContextHolder.getContext().getAuthentication();
        assertSame( SecurityContextHolder.getContext(), taskCommand.getSecurityContext() );

        this.runAsAdmin();
        Authentication launchingUserAuth = SecurityContextHolder.getContext().getAuthentication();

        assertNotSame( executingUserAuth, launchingUserAuth );

        Task<TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        final Authentication[] authenticationAfterInitialize = new Authentication[1];
        final Authentication[] authenticationDuringProgress = new Authentication[1];
        final Authentication[] authenticationAfterSuccess = new Authentication[1];
        final Authentication[] authenticationAfterComplete = new Authentication[1];

        ExecutingTask executingTask = new ExecutingTask( TaskUtils.generateTaskId(), task );
        executingTask.setLifecycleHandler( new TaskLifecycleHandler() {

            @Override
            public void onStart() {
                authenticationAfterInitialize[0] = SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public void onProgress( String message ) {
                authenticationDuringProgress[0] = SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public void onFailure( Exception e ) {
                fail();
            }

            @Override
            public void onSuccess() {
                authenticationAfterComplete[0] = SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public void onComplete() {
                authenticationAfterSuccess[0] = SecurityContextHolder.getContext().getAuthentication();
            }
        } );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( new DelegatingSecurityContextCallable<>( executingTask, taskCommand.getSecurityContext() ) );
        this.tryGetAnswer( future );

        assertSame( executingUserAuth, authenticationAfterInitialize[0] );
        assertSame( executingUserAuth, authenticationAfterSuccess[0] );
        assertSame( executingUserAuth, authenticationAfterComplete[0] );
        assertSame( executingUserAuth, authenticationDuringProgress[0] );
    }

    private void tryGetAnswer( Future<TaskResult> future ) {
        try {
            TaskResult taskResult = future.get();
            String answer = ( String ) taskResult.getAnswer();
            assertEquals( "SUCCESS", answer );
        } catch ( InterruptedException | ExecutionException e ) {
            fail();
        }
    }

    private static class FailureTestTask extends AbstractTask<TaskCommand> {

        private static final Log log = LogFactory.getLog( FailureTestTask.class );

        @Override
        public TaskResult call() {
            log.info( "Executing a failing task." );
            throw new AccessDeniedException( "Test!" );
        }
    }

    private static class SuccessTestTask extends AbstractTask<TaskCommand> {

        private static final Log log = LogFactory.getLog( SuccessTestTask.class );

        @Override
        public TaskResult call() {
            log.info( "Executing a success task." );
            return newTaskResult( "SUCCESS" );
        }
    }

    private static class TestTaskCommand extends TaskCommand {

    }
}
