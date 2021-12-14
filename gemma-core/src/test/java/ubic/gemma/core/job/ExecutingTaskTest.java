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
import org.springframework.security.core.context.SecurityContext;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @author anton
 */
public class ExecutingTaskTest extends BaseSpringContextTest {

    private final AtomicBoolean onStartRan = new AtomicBoolean( false );
    private final AtomicBoolean onFailureRan = new AtomicBoolean( false );
    private final AtomicBoolean onFinishRan = new AtomicBoolean( false );
    private final AtomicBoolean progressUpdateCallbackInvoked = new AtomicBoolean( false );
    private final ExecutingTask.TaskLifecycleHandler statusUpdateHandler = new ExecutingTask.TaskLifecycleHandler() {
        @Override
        public void onFailure( Throwable e ) {
            onFailureRan.set( true );
        }

        @Override
        public void onFinish() {
            onFinishRan.set( true );
        }

        @Override
        public void onStart() {
            onStartRan.set( true );
        }
    };
    @Autowired
    UserManager userManager;
    private SecurityContext securityContextAfterInitialize;
    private SecurityContext securityContextAfterFinish;
    private final ExecutingTask.TaskLifecycleHandler statusSecurityContextCheckerHandler = new ExecutingTask.TaskLifecycleHandler() {
        @Override
        public void onFailure( Throwable e ) {
            securityContextAfterFinish = SecurityContextHolder.getContext();
        }

        @Override
        public void onFinish() {
            securityContextAfterFinish = SecurityContextHolder.getContext();
        }

        @Override
        public void onStart() {
            securityContextAfterInitialize = SecurityContextHolder.getContext();
        }
    };
    private SecurityContext securityContextAfterFail;

    @Before
    public void setUp() {
        Assume.assumeTrue( "These tests must be run with -Dlog4j1.compatibility=true",
                Objects.equals( System.getProperty( "log4j1.compatibility" ), "true" ) );

        onStartRan.set( false );
        onFailureRan.set( false );
        onFinishRan.set( false );

        progressUpdateCallbackInvoked.set( false );

        securityContextAfterInitialize = null;
        securityContextAfterFinish = null;
        securityContextAfterFail = null;
    }

    @Test
    public void testOrderOfExecutionFailure() {
        TaskCommand taskCommand = new TaskCommand();
        Task<TaskResult, TaskCommand> task = new FailureTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setProgressUpdateCallback( message -> progressUpdateCallbackInvoked.set( true ) );
        executingTask.setStatusCallback( statusUpdateHandler );

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

        assertTrue( progressUpdateCallbackInvoked.get() );

        assertTrue( onStartRan.get() );
        assertFalse( onFinishRan.get() );
        assertTrue( onFailureRan.get() );
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
        executingTask.setProgressUpdateCallback( message -> progressUpdateCallbackInvoked.set( true ) );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        this.tryGetAnswer( future );

        assertTrue( progressUpdateCallbackInvoked.get() );

        assertTrue( onStartRan.get() );
        assertTrue( onFinishRan.get() );
        assertFalse( onFailureRan.get() );
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
        this.runAsAdmin();

        Task<TaskResult, TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setProgressUpdateCallback( message -> progressUpdateCallbackInvoked.set( true ) );
        executingTask.setStatusCallback( statusSecurityContextCheckerHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        this.tryGetAnswer( future );

        assertEquals( taskCommand.getSecurityContext(), securityContextAfterInitialize );
        assertNotSame( taskCommand.getSecurityContext(), securityContextAfterFinish );
        assertNull( securityContextAfterFail );
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
