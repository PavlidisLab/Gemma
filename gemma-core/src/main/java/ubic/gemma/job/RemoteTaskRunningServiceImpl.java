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
package ubic.gemma.job;

import com.google.common.util.concurrent.*;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;
import ubic.gemma.job.progress.grid.ProgressUpdateCallback;
import ubic.gemma.job.progress.grid.RemoteProgressAppender;
import ubic.gemma.tasks.Task;
import ubic.gemma.util.MailUtils;

import javax.jms.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This will run on remote worker jvm
 *
 * CompletionService backed by ThreadPoolExecutor is used to execute tasks.
 *
 *
 *
 *
 * ActiveMQ communication.
 *
 * ApplicationContext is injectected and is used to load classes implementing Tasks.
 *
 *
 */
@Component
public class RemoteTaskRunningServiceImpl implements RemoteTaskRunningService {
    private static Log log = LogFactory.getLog( RemoteTaskRunningServiceImpl.class );

    @Autowired @Qualifier("amqJmsTemplate") private JmsTemplate amqJmsTemplate;
    @Autowired private MailUtils mailUtils;

    @Autowired private ApplicationContext applicationContext;

    private final Map<String,ListenableFuture> taskIdToFuture = new ConcurrentHashMap<String, ListenableFuture>();
    private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();

    private final ListeningExecutorService executorService =
            MoreExecutors.listeningDecorator( new ThreadPoolExecutor( 3, 15, 10, TimeUnit.MINUTES, workQueue ) );

    // Take completed Task and send its result to the SubmittedTaskProxy.
    private FutureCallback<TaskResult> sendTaskResultCallback = new FutureCallback<TaskResult>() {
        @Override
        public void onSuccess( TaskResult taskResult ) {
            // Clean up taskIdToFuture map
            taskIdToFuture.remove( taskResult.getTaskId() );
            Destination resultDestination = new ActiveMQQueue( "task.result." + taskResult.getTaskId() );
            sendMessage( resultDestination, taskResult );
        }

        @Override
        public void onFailure( Throwable throwable ) {
            log.error( throwable );
        }
    };

    // maintains task submission queue
    @Override
    public void submit( final TaskCommand taskCommand ) {
        if ( taskCommand == null ) throw new NullPointerException("taskCommand cannot be null.");

        final String taskId = taskCommand.getTaskId();
        assert taskId != null;

        final Task task = matchTaskCommandToTask( taskCommand );
        if (task == null) throw new IllegalArgumentException( "Can't find bean for Task "+ taskCommand.getTaskClass().getSimpleName() );
        task.setCommand( taskCommand );

        //TODO: somehow make sure they are set in the same spot for this AND proxy object on the client side
        //TODO: config file? properties?
        final Destination lifeCycleQueue = new ActiveMQQueue( "task.lifeCycle."+taskId );
        final Destination progressUpdatesQueue = new ActiveMQQueue( "task.progress."+taskId );

        final java.util.Queue<String> progressUpdatesQueueLocal = new ConcurrentLinkedQueue<String>();

        // Comes from log appender that's attached to standard logger.
        final ProgressUpdateCallback progressUpdateCallback = new ProgressUpdateCallback() {
            @Override
            public void addProgressUpdate( String message ) {
                sendMessage( progressUpdatesQueue, message );
                progressUpdatesQueueLocal.add( message );
            }
        };


        //TODO: this kind of setup can be moved to subclasses?
        // Set up logger for remote task.
        ExecutingTask.ProgressUpdateAppender progressUpdateAppender = new ExecutingTask.ProgressUpdateAppender() {
            private final RemoteProgressAppender logAppender = new RemoteProgressAppender( taskId, progressUpdateCallback );

            @Override public void initialize() {
                logAppender.initialize();
            }

            @Override public void tearDown() {
                logAppender.close();
            }
        };

        final ExecutingTask executingTask = new ExecutingTask( task, taskCommand );
        executingTask.setProgressAppender( progressUpdateAppender );
        executingTask.setLocalProgressQueue ( progressUpdatesQueueLocal );

        ExecutingTask.TaskLifecycleHandler statusRemoteCallback = new ExecutingTask.TaskLifecycleHandler() {
            @Override public void onStart() {
                TaskStatusUpdate statusUpdate = new TaskStatusUpdate( SubmittedTask.Status.RUNNING, new Date() );
                sendMessage( lifeCycleQueue, statusUpdate );
            }
            @Override public void onFinish() {
                TaskStatusUpdate statusUpdate = new TaskStatusUpdate( SubmittedTask.Status.DONE, new Date() );
                sendMessage( lifeCycleQueue, statusUpdate );
            }
            @Override public void onFailure( Throwable e ) {
                TaskStatusUpdate statusUpdate = new TaskStatusUpdate( SubmittedTask.Status.FAILED, new Date() );
                sendMessage( lifeCycleQueue, statusUpdate );
            }
        };

        executingTask.setStatusCallback( statusRemoteCallback );

        ListenableFuture future = executorService.submit( executingTask );
        // These are run on task completion.
        Futures.addCallback( future, sendTaskResultCallback );
        if ( taskCommand.isEmailAlert() ) {
            Futures.addCallback( future, emailNotificationCallback );
        }

        taskIdToFuture.put( taskId, future );
    }

    @Override
    public void cancelQueuedTask( String taskId ) {
        // TODO: It wasn't possible in the past. This a new feature.
        throw new UnsupportedOperationException( "cancelQueuedTask is not currently implemented" );
    }

    @Override
    public ListenableFuture<TaskResult> getRunningTaskFuture( String taskId ) {
        return taskIdToFuture.get( taskId );
    }

    @Override
    public void shutdown() {
        this.executorService.shutdownNow();
    }

    public void sendMessage( Destination destination, final Serializable object ) {
        amqJmsTemplate.send( destination, new MessageCreator() {
            @Override
            public Message createMessage( Session session ) throws JMSException {
                ObjectMessage message = session.createObjectMessage( object );
                return message;
            }
        } );
    }

    //TODO: these methods below are shared by both local and remote task running services -> extract into separate class
    private Task matchTaskCommandToTask( TaskCommand taskCommand ) {
        Class taskClass = taskCommand.getTaskClass();
        if (taskClass == null) throw new IllegalArgumentException( "Task is not set for "
                + taskCommand.getClass().getSimpleName() );

        // TODO: Try using @Configurable and new operator in the future.
        Task task = (Task) applicationContext.getBean( taskClass );
        if (task == null) throw new IllegalArgumentException( "Task bean is not found for "
                + taskClass.getSimpleName() );

        task.setCommand( taskCommand );
        return task;
    }


    private FutureCallback<TaskResult> emailNotificationCallback = new FutureCallback<TaskResult>() {
        @Override
        public void onSuccess( TaskResult taskResult ) {
            mailUtils.sendTaskCompletedNotificationEmail( taskResult );
        }

        @Override
        public void onFailure( Throwable throwable ) {
            log.error( "Shouldn't happen since we take care of exceptions inside ExecutingTask. "
                    + throwable.getMessage() );
        }
    };

        //TODO: One idea to move this to the web app is to send jms messages on a special topic which mailengine would listen on.
//    private void emailNotifyCompletionOfTask( TaskCommand taskCommand, ExecutingTask executingTask ) {
//        if ( StringUtils.isNotBlank( taskCommand.getSubmitter() ) ) {
//            User user = userService.findByUserName( taskCommand.getSubmitter() );
//
//            assert user != null;
//
//            String emailAddress = user.getEmail();
//
//            if ( emailAddress != null ) {
//                log.debug( "Sending email notification to " + emailAddress );
//                SimpleMailMessage msg = new SimpleMailMessage();
//                msg.setTo( emailAddress );
//                msg.setFrom( ConfigUtils.getAdminEmailAddress() );
//                msg.setSubject( "Gemma task completed" );
//
//                String logs = "Event logs:\n";
//                if ( executingTask != null ) {
//                    logs += StringUtils.join( executingTask.getLocalProgressQueue(), "\n" );
//                }
//
//                msg.setText( "A job you started on Gemma is completed (taskid=" + taskCommand.getTaskId() + ", "
//                        + taskCommand.getTaskClass().getSimpleName() + ")\n\n" + logs + "\n" );
//
//                /*
//                 * TODO provide a link to something relevant something like:
//                 */
//                String url = ConfigUtils.getBaseUrl() + "user/tasks.html?taskId=" + taskCommand.getTaskId();
//
//                mailEngine.send( msg );
//            }
//        }
//    }
}
