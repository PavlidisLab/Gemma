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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;
import ubic.gemma.job.grid.util.JMSBrokerMonitor;
import ubic.gemma.job.progress.LocalProgressAppender;
import ubic.gemma.tasks.Task;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.MailUtils;

import javax.jms.*;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

/**
 * Handles the execution of tasks in threads that can be checked by clients later.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class TaskRunningServiceImpl implements TaskRunningService {

    private static final Log log = LogFactory.getLog( TaskRunningServiceImpl.class );

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private MailUtils mailUtils;

    @Autowired
    private JMSBrokerMonitor jmsBrokerMonitor;
    @Autowired(required = false)
    @Qualifier("amqJmsTemplate")
    private JmsTemplate jmsTemplate;
    @Autowired
    @Qualifier("taskSubmissionDestination")
    private Destination taskSubmissionQueue;

    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator( Executors
            .newFixedThreadPool( 20 ) );

    /**
     *
     */
    private final Map<String, SubmittedTask> submittedTasks = new ConcurrentHashMap<String, SubmittedTask>();

    @Override
    public Collection<SubmittedTask> getSubmittedTasks() {
        return submittedTasks.values();
    }

    @Override
    public SubmittedTask getSubmittedTask( String taskId ) {
        return submittedTasks.get( taskId );
    }

    @Override
    public String submitLocalJob( BackgroundJob job ) throws ConflictingTaskException {
        if ( job == null ) throw new IllegalArgumentException( "Must provide a job" );

        final TaskCommand taskCommand = job.getCommand();
        final String taskId = taskCommand.getTaskId();
        log.debug( "Submitting " + taskId );

        assert taskCommand.getSecurityContext() != null;
        assert taskCommand.getSecurityContext().getAuthentication() != null;
        assert taskCommand.getTaskId() != null;

        final Queue<String> progressUpdates = new ConcurrentLinkedQueue<String>();

        ExecutingTask.ProgressUpdateAppender progressUpdateAppender = new ExecutingTask.ProgressUpdateAppender() {
            private final LocalProgressAppender logAppender = new LocalProgressAppender( taskId, progressUpdates );

            @Override
            public void initialize() {
                logAppender.initialize();
            }

            @Override
            public void tearDown() {
                logAppender.close();
            }
        };

        final SubmittedTaskLocal submittedTask = new SubmittedTaskLocal( taskCommand, progressUpdates, this );

        ExecutingTask.TaskLifecycleHandler statusCallback = new ExecutingTask.TaskLifecycleHandler() {
            @Override
            public void onStart() {
                submittedTask.setStatus( SubmittedTask.Status.RUNNING );
                submittedTask.setStartTime( new Date() );
            }

            @Override
            public void onFinish() {
                submittedTask.setStatus( SubmittedTask.Status.DONE );
                submittedTask.setFinishTime( new Date() );
            }

            @Override
            public void onFailure( Throwable e ) {
                submittedTask.setStatus( SubmittedTask.Status.FAILED );
                submittedTask.setFinishTime( new Date() );
            }
        };

        ExecutingTask executingTask = new ExecutingTask( job );
        executingTask.setStatusCallback( statusCallback );
        executingTask.setProgressAppender( progressUpdateAppender );

        ListenableFuture<TaskResult> future = executorService.submit( executingTask );
        // Adding post-processing steps, they will run on future completion.
        // Currently we have only email notification.
        if ( taskCommand.isEmailAlert() ) {
            Futures.addCallback( future, emailNotifcationFutureCallback );
        }

        submittedTask.setFuture( future );

        submittedTasks.put( taskId, submittedTask );
        return job.getTaskId();
    }

    @Override
    public void cancelTask( SubmittedTask task ) {
        task.cancel();
    }

    @Override
    public void addEmailNotificationFutureCallback( ListenableFuture<TaskResult> future ) {
        Futures.addCallback( future, emailNotifcationFutureCallback );
    }

    // Reused by multiple threads.
    private FutureCallback<TaskResult> emailNotifcationFutureCallback = new FutureCallback<TaskResult>() {

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

    @Override
    public String submitLocalTask( TaskCommand taskCommand ) throws ConflictingTaskException {
        final Task task = matchTaskCommandToTask( taskCommand );

        BackgroundJob<TaskCommand, TaskResult> job = new BackgroundJob<TaskCommand, TaskResult>( taskCommand ) {
            @Override
            protected TaskResult processJob() {
                return task.execute();
            }
        };

        return submitLocalJob( job );
    }

    /**
     * Run remotely if possible, otherwise run locally.
     * 
     * @param taskCommand
     * @return
     * @throws ConflictingTaskException
     */
    @Override
    public String submitRemoteTask( final TaskCommand taskCommand ) throws ConflictingTaskException {

        if ( ConfigUtils.isRemoteTasksEnabled() && jmsBrokerMonitor.canServiceRemoteTasks() ) {
            jmsTemplate.send( taskSubmissionQueue, new MessageCreator() {
                @Override
                public Message createMessage( Session session ) throws JMSException {
                    ObjectMessage message = session.createObjectMessage( taskCommand );
                    return message;
                }
            } );

            SubmittedTask submittedTask = new SubmittedTaskProxy( taskCommand, jmsTemplate );
            submittedTasks.put( taskCommand.getTaskId(), submittedTask );

            return taskCommand.getTaskId();
        } else {
            return this.submitLocalTask( taskCommand );
        }
    }

    private Task matchTaskCommandToTask( TaskCommand taskCommand ) {
        Class taskClass = taskCommand.getTaskClass();
        if ( taskClass == null )
            throw new IllegalArgumentException( "Task is not set for " + taskCommand.getClass().getSimpleName() );

        // TODO: Try using @Configurable and new operator in the future.
        Task task = ( Task ) applicationContext.getBean( taskClass );
        if ( task == null )
            throw new IllegalArgumentException( "Task bean is not found for " + taskClass.getSimpleName() );

        task.setCommand( taskCommand );
        return task;
    }

    // /**
    // * @param taskId
    // * @param result. If the submitter is blank, this doesn't do anything.
    // */
    // private void emailNotifyCompletionOfTask( TaskCommand taskCommand ) {
    // if ( StringUtils.isNotBlank( taskCommand.getSubmitter() ) ) {
    // User user = userService.findByUserName( taskCommand.getSubmitter() );
    //
    // assert user != null;
    //
    // String emailAddress = user.getEmail();
    //
    // if ( emailAddress != null ) {
    // log.debug( "Sending email notification to " + emailAddress );
    // SimpleMailMessage msg = new SimpleMailMessage();
    // msg.setTo( emailAddress );
    // msg.setFrom( ConfigUtils.getAdminEmailAddress() );
    // msg.setSubject( "Gemma task completed" );
    // SubmittedTask submittedTask = this.getSubmittedTask( taskCommand.getTaskId() );
    //
    // String logs = "Event logs:\n";
    // if ( submittedTask != null ) {
    // logs += StringUtils.join( submittedTask.getProgressUpdates(), "\n" );
    // }
    //
    // msg.setText( "A job you started on Gemma is completed (taskid=" + taskCommand.getTaskId() + ", "
    // + taskCommand.getTaskClass().getSimpleName() + ")\n\n" + logs + "\n" );
    //
    // /*
    // * TODO provide a link to something relevant something like:
    // */
    // String url = ConfigUtils.getBaseUrl() + "user/tasks.html?taskId=" + taskCommand.getTaskId();
    //
    // mailEngine.send( msg );
    // }
    // }
    // }
}
