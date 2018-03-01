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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.core.job.executor.common.ExecutingTask;
import ubic.gemma.core.job.executor.common.ProgressUpdateAppender;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.core.testing.BaseSpringContextTest;

import java.util.Date;
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
    private final AtomicBoolean appenderInitialized = new AtomicBoolean( false );
    private final AtomicBoolean appenderTakenDown = new AtomicBoolean( false );
    private final ProgressUpdateAppender progressAppender = new ProgressUpdateAppender() {
        @Override
        public void initialize() {
            appenderInitialized.set( true );
        }

        @Override
        public void tearDown() {
            appenderTakenDown.set( true );
        }
    };
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
        onStartRan.set( false );
        onFailureRan.set( false );
        onFinishRan.set( false );

        appenderInitialized.set( false );
        appenderTakenDown.set( false );

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
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            Throwable exception = taskResult.getException();
            assertEquals( AccessDeniedException.class, exception.getClass() );
            assertEquals( "Test!", exception.getMessage() );
        } catch ( InterruptedException | ExecutionException e ) {
            fail();
        }

        assertTrue( appenderInitialized.get() );
        assertTrue( appenderTakenDown.get() );

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
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        this.tryGetAnswer( future );

        assertTrue( appenderInitialized.get() );
        assertTrue( appenderTakenDown.get() );

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
                    new UserDetailsImpl( "foo", "ExecutingTaskTestUser", true, null, "fooUser@chibi.ubc.ca", "key",
                            new Date() ) );
        }

        this.runAsUser( "ExecutingTaskTestUser" );
        TaskCommand taskCommand = new TaskCommand();
        this.runAsAdmin();

        Task<TaskResult, TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusSecurityContextCheckerHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        this.tryGetAnswer( future );

        assertEquals( taskCommand.getSecurityContext(), securityContextAfterInitialize );
        assertNotSame( taskCommand.getSecurityContext(), securityContextAfterFinish );
        assertEquals( null, securityContextAfterFail );
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

        @Override
        public TaskResult execute() {
            throw new AccessDeniedException( "Test!" );
        }
    }

    private static class SuccessTestTask extends AbstractTask<TaskResult, TaskCommand> {

        @Override
        public TaskResult execute() {
            return new TaskResult( this.getTaskCommand(), "SUCCESS" );
        }
    }
}
