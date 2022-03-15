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

import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.core.job.executor.common.ExecutingTask;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author anton
 */
public class ExecutingTaskTest extends BaseSpringContextTest {

    @Autowired
    UserManager userManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Assume.assumeTrue( "These tests must be run with -Dlog4j1.compatibility=true",
                Objects.equals( System.getProperty( "log4j1.compatibility" ), "true" ) );
    }

    @Test
    public void testOrderOfExecutionFailure() {
        TaskCommand taskCommand = new TaskCommand();
        Task<TaskResult, TaskCommand> task = new FailureTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        ExecutingTask.TaskLifecycleHandler lifecycleHandler = mock( ExecutingTask.TaskLifecycleHandler.class );
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
        Throwable exception = taskResult.getException();
        assertEquals( AccessDeniedException.class, exception.getClass() );
        assertEquals( "Test!", exception.getMessage() );

        verify( lifecycleHandler ).onStart();
        verify( lifecycleHandler ).onProgress( "Executing a failing task." );
        verify( lifecycleHandler ).onFailure( any( Throwable.class ) );
        verify( lifecycleHandler ).onComplete();
    }

    // TODO: Test security context under different failure scenarios
    // - exception in the task
    // - exception in the lifecycle hook
    // - exception in progress appender setup/teardown hook
    @Test
    public void testOrderOfExecutionSuccess() {
        TaskCommand taskCommand = new TaskCommand();
        Task<TaskResult, TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        ExecutingTask.TaskLifecycleHandler lifecycleHandler = mock( ExecutingTask.TaskLifecycleHandler.class );
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

        try {
            this.userManager.loadUserByUsername( "ExecutingTaskTestUser" );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser(
                    new UserDetailsImpl( "foo", "ExecutingTaskTestUser", true, null, "fooUser@gemma.msl.ubc.ca", "key",
                            new Date() ) );
        }

        this.runAsUser( "ExecutingTaskTestUser" );
        TaskCommand taskCommand = new TaskCommand();
        Authentication executingUserAuth = SecurityContextHolder.getContext().getAuthentication();

        this.runAsAdmin();
        Authentication launchingUserAuth = SecurityContextHolder.getContext().getAuthentication();

        assertNotSame( executingUserAuth, launchingUserAuth );

        Task<TaskResult, TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        final Authentication[] authenticationAfterInitialize = new Authentication[1];
        final Authentication[] authenticationDuringProgress = new Authentication[1];
        final Authentication[] authenticationAfterSuccess = new Authentication[1];
        final Authentication[] authenticationAfterComplete = new Authentication[1];

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setLifecycleHandler( new ExecutingTask.TaskLifecycleHandler() {

            @Override
            public void onStart() {
                authenticationAfterInitialize[0] = SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public void onProgress( String message ) {
                authenticationDuringProgress[0] = SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public void onFailure( Throwable e ) {
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
        Future<TaskResult> future = executorService.submit( executingTask );
        this.tryGetAnswer( future );

        assertSame( launchingUserAuth, authenticationAfterInitialize[0] );
        assertSame( launchingUserAuth, authenticationAfterSuccess[0] );
        assertSame( launchingUserAuth, authenticationAfterComplete[0] );
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

    private static class FailureTestTask extends AbstractTask<TaskResult, TaskCommand> {

        private static Log log = LogFactory.getLog( FailureTestTask.class );

        @Override
        public TaskResult execute() {
            log.info( "Executing a failing task." );
            throw new AccessDeniedException( "Test!" );
        }
    }

    private static class SuccessTestTask extends AbstractTask<TaskResult, TaskCommand> {

        private static Log log = LogFactory.getLog( SuccessTestTask.class );

        @Override
        public TaskResult execute() {
            log.info( "Executing a success task." );
            return new TaskResult( this.getTaskCommand(), "SUCCESS" );
        }
    }
}
