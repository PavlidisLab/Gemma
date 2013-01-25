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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ubic.gemma.job.grid.util.JMSBrokerMonitor;
import ubic.gemma.job.progress.LocalProgressAppender;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.tasks.Task;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.MailEngine;

import javax.annotation.PostConstruct;
import javax.jms.*;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Handles the execution of tasks in threads that can be check by clients later.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class TaskRunningServiceImpl implements TaskRunningService {

    private static Log log = LogFactory.getLog( TaskRunningServiceImpl.class );

    private final Map<String,SubmittedTask> submittedTasks= new ConcurrentHashMap<String, SubmittedTask>();

    @Autowired private MailEngine mailEngine;
    @Autowired private UserManager userService;
    @Autowired private JMSBrokerMonitor jmsBrokerMonitor;
    @Autowired @Qualifier("amqJmsTemplate") private JmsTemplate jmsTemplate;

    @Autowired private ApplicationContext applicationContext;

    @Autowired @Qualifier("taskSubmissionDestination") private Destination taskSubmissionQueue;

    @PostConstruct
    public void initMaintenanceThread() {
        // Start a thread to monitor finished tasks that have not been retrieved.
        Thread maintenanceThread = new Thread( new SubmittedTasksMaintenanceThread( submittedTasks, this ) );
        maintenanceThread.setDaemon( true );
        maintenanceThread.setName( "TaskSweepup" );
        maintenanceThread.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#getSubmittedTasks()
     */
    @Override
    public Collection<SubmittedTask> getSubmittedTasks() {
        return submittedTasks.values();
    }

    @Override
    public SubmittedTask getSubmittedTask(String taskId) {
        return submittedTasks.get( taskId );
    }

    /*
 * (non-Javadoc)
 *
 * @see ubic.gemma.job.TaskRunningService#cancelTask(java.lang.String)
 */
    @Override
    public boolean cancelTask( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );

        log.debug( "Cancelling " + taskId );
        boolean cancelled = false;

        SubmittedTask submittedTask = this.submittedTasks.get( taskId );
        if (submittedTask == null) throw new IllegalArgumentException("couldn't find task with task id:"+taskId);

        submittedTask.cancel();

        return cancelled;
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
        final LocalProgressAppender logAppender = new LocalProgressAppender( taskId, progressUpdates );

        ExecutingTask.ProgressUpdateAppender progressUpdateAppender = new ExecutingTask.ProgressUpdateAppender() {
            @Override public void initialize() {
                Logger logger = LogManager.getLogger( "ubic.gemma" );
                Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
                logger.addAppender( logAppender );
                baseCodeLogger.addAppender( logAppender );
            }

            @Override public void tearDown() {
                Logger logger = LogManager.getLogger( "ubic.gemma" );
                Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
                logger.removeAppender( logAppender );
                baseCodeLogger.removeAppender( logAppender );
            }
        };

        final SubmittedTaskLocal submittedTask = new SubmittedTaskLocal( taskCommand, progressUpdates );

        ExecutingTask.TaskLifecycleCallback statusCallback = new ExecutingTask.TaskLifecycleCallback() {
            @Override public void beforeRun() {
                submittedTask.setStatus( SubmittedTask.Status.RUNNING );
                submittedTask.setStartTime( new Date() );

                SecurityContextHolder.setContext( taskCommand.getSecurityContext() );
            }
            @Override public void afterRun() {
                submittedTask.setStatus( SubmittedTask.Status.DONE );
                submittedTask.setFinishTime( new Date() );

                SecurityContextHolder.clearContext();

                if (submittedTask.isEmailAlert()) {
                    emailNotifyCompletionOfTask( taskCommand );
                }
            }
            @Override public void onFailure( Throwable e ) {
                submittedTask.setStatus( SubmittedTask.Status.FAILED );
                submittedTask.setFinishTime( new Date() );

                SecurityContextHolder.clearContext();

                if (submittedTask.isEmailAlert()) {
                    emailNotifyCompletionOfTask( taskCommand );
                }
            }
        };

        ExecutingTask executingTask = new ExecutingTask( job );
        executingTask.setStatusCallback( statusCallback );
        executingTask.setProgressAppender( progressUpdateAppender );

        // Run the task in its own thread.
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit( executingTask );
        submittedTask.setFuture( future );

        submittedTasks.put( taskId, submittedTask );
        return job.getTaskId();
    }

    @Override
    public void cancelTask( SubmittedTask task ) {
        task.cancel();
    }

    @Override
    public String submitLocalTask( TaskCommand taskCommand ) throws ConflictingTaskException {
        final Task task = matchTaskCommandToTask( taskCommand );

        BackgroundJob<TaskCommand, TaskResult> job = new BackgroundJob<TaskCommand, TaskResult>(taskCommand) {
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
     *
     * @param taskCommand
     * @return
     * @throws ConflictingTaskException
     */
    @Override
    public String submitRemoteTask( final TaskCommand taskCommand ) throws ConflictingTaskException {

        if ( ConfigUtils.isRemoteTasksEnabled() && jmsBrokerMonitor.canServiceRemoteTasks() ) {
            jmsTemplate.send(taskSubmissionQueue, new MessageCreator() {
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
        if (taskClass == null) throw new IllegalArgumentException( "Task is not set for "
                + taskCommand.getClass().getSimpleName() );

        // TODO: Try using @Configurable and new operator in the future.
        Task task = (Task) applicationContext.getBean( taskClass );
        if (task == null) throw new IllegalArgumentException( "Task bean is not found for "
                + taskClass.getSimpleName() );

        task.setCommand( taskCommand );
        return task;
    }

    /**
     * Test whether the job can be run at this time. Checks for already-running tasks of the same type owned by the same
     * user etc.
     */

    /**
     * @param taskId
     * @param result. If the submitter is blank, this doesn't do anything.
     */
    private void emailNotifyCompletionOfTask( TaskCommand taskCommand ) {
        if ( StringUtils.isNotBlank( taskCommand.getSubmitter() ) ) {
            User user = userService.findByUserName( taskCommand.getSubmitter() );

            assert user != null;

            String emailAddress = user.getEmail();

            if ( emailAddress != null ) {
                log.debug( "Sending email notification to " + emailAddress );
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo( emailAddress );
                msg.setFrom( ConfigUtils.getAdminEmailAddress() );
                msg.setSubject( "Gemma task completed" );
                SubmittedTask submittedTask =this.getSubmittedTask( taskCommand.getTaskId() );

                String logs = "Event logs:\n";
                if ( submittedTask != null ) {
                    logs += StringUtils.join( submittedTask.getProgressUpdates(), "\n" );
                }

                msg.setText( "A job you started on Gemma is completed (taskid=" + taskCommand.getTaskId() + ", "
                        + taskCommand.getTaskClass().getSimpleName() + ")\n\n" + logs + "\n" );

                /*
                 * TODO provide a link to something relevant something like:
                 */
                String url = ConfigUtils.getBaseUrl() + "user/tasks.html?taskId=" + taskCommand.getTaskId();

                mailEngine.send( msg );
            }
        }
    }
}
