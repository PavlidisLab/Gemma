package ubic.gemma.job;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.job.executor.common.ExecutingTask;
import ubic.gemma.job.executor.common.ProgressUpdateAppender;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.tasks.Task;
import ubic.gemma.testing.BaseSpringContextTest;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 02/02/13
 * Time: 10:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExecutingTaskTest extends BaseSpringContextTest {

    @Autowired UserManager userManager;

    private AtomicBoolean onStartRan = new AtomicBoolean( false );
    private AtomicBoolean onFailureRan = new AtomicBoolean( false );
    private AtomicBoolean onFinishRan = new AtomicBoolean( false );

    private AtomicBoolean appenderInitialized = new AtomicBoolean( false );
    private AtomicBoolean appenderTakenDown = new AtomicBoolean( false );

    private SecurityContext securityContextAfterInitialize;
    private SecurityContext securityContextAfterFinish;
    private SecurityContext securityContextAfterFail;


    private ProgressUpdateAppender progressAppender = new ProgressUpdateAppender() {
        @Override
        public void initialize() {
            appenderInitialized.set( true );
        }

        @Override
        public void tearDown() {
            appenderTakenDown.set( true );
        }
    };

    private ExecutingTask.TaskLifecycleHandler statusUpdateHandler = new ExecutingTask.TaskLifecycleHandler() {
        @Override
        public void onStart() {
            onStartRan.set( true );
        }

        @Override
        public void onFinish() {
            onFinishRan.set( true );
        }

        @Override
        public void onFailure( Throwable e ) {
            onFailureRan.set( true );
        }
    };


    private ExecutingTask.TaskLifecycleHandler statusSecurityContextCheckerHandler = new ExecutingTask.TaskLifecycleHandler() {
        @Override
        public void onStart() {
            securityContextAfterInitialize = SecurityContextHolder.getContext();
        }

        @Override
        public void onFinish() {
            securityContextAfterFinish = SecurityContextHolder.getContext();
        }

        @Override
        public void onFailure( Throwable e ) {
            securityContextAfterFinish = SecurityContextHolder.getContext();
        }
    };


    private static class SuccessTestTask implements Task<TaskResult, TaskCommand> {
        TaskCommand command;

        @Override
        public void setCommand( TaskCommand taskCommand ) {
            command = taskCommand;
        }

        @Override
        public TaskResult execute() {
            return new TaskResult( command, "SUCCESS" );
        }
    }

    private static class FailureTestTask implements Task<TaskResult, TaskCommand> {
        TaskCommand command;

        @Override
        public void setCommand( TaskCommand taskCommand ) {
            command = taskCommand;
        }

        @Override
        public TaskResult execute() {
            throw new AccessDeniedException("Test!");
        }
    }

    @Before
    public void setUp() throws Exception {
        onStartRan.set( false );
        onFailureRan.set( false );
        onFinishRan.set( false );

        appenderInitialized.set( false );
        appenderTakenDown.set( false );

        securityContextAfterInitialize = null;
        securityContextAfterFinish = null;
        securityContextAfterFail = null;
    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testSecurityContextManagement() {
        try {
            this.userManager.loadUserByUsername( "ExecutingTaskTestUser" );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", "ExecutingTaskTestUser", true, null, "fooUser@chibi.ubc.ca", "key",
                    new Date() ) );
        }

        runAsUser( "ExecutingTaskTestUser" );
        TaskCommand taskCommand = new TaskCommand();
        runAsAdmin();

        Task task = new SuccessTestTask();
        task.setCommand( taskCommand );

        ExecutingTask executingTask = new ExecutingTask( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusSecurityContextCheckerHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            String answer = (String) taskResult.getAnswer();
            assertEquals( "SUCCESS", answer );
        } catch (InterruptedException e) {
            fail();
        } catch (ExecutionException e) {
            fail();
        }

        assertEquals( taskCommand.getSecurityContext(), securityContextAfterInitialize );
        assertNotSame( taskCommand.getSecurityContext(), securityContextAfterFinish );
        assertEquals( null, securityContextAfterFail );
    }

    //TODO: Test security context under different failure scenarios
    //  - exception in the task
    //  - exception in the lifecycle hook
    //  - exception in progress appender setup/teardown hook
    @Test
    public void testOrderOfExecutionSuccess() {
        TaskCommand taskCommand = new TaskCommand();
        Task task = new SuccessTestTask();
        task.setCommand( taskCommand );

        ExecutingTask executingTask = new ExecutingTask( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            String answer = (String) taskResult.getAnswer();
            assertEquals( "SUCCESS", answer );
        } catch (InterruptedException e) {
            fail();
        } catch (ExecutionException e) {
            fail();
        }

        assertTrue( appenderInitialized.get() );
        assertTrue( appenderTakenDown.get() );

        assertTrue( onStartRan.get() );
        assertTrue( onFinishRan.get() );
        assertFalse( onFailureRan.get() );
    }

    @Test
    public void testOrderOfExecutionFailure() {
        TaskCommand taskCommand = new TaskCommand();
        Task task = new FailureTestTask();
        task.setCommand( taskCommand );

        ExecutingTask executingTask = new ExecutingTask( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            Throwable exception = taskResult.getException();
            assertEquals( AccessDeniedException.class, exception.getClass() );
            assertEquals( "Test!", exception.getMessage() );
        } catch (InterruptedException e) {
            fail();
        } catch (ExecutionException e) {
            fail();
        }

        assertTrue( appenderInitialized.get() );
        assertTrue( appenderTakenDown.get() );

        assertTrue( onStartRan.get() );
        assertFalse( onFinishRan.get() );
        assertTrue( onFailureRan.get() );
    }

}
