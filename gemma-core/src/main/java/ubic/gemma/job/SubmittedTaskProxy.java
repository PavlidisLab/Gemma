package ubic.gemma.job;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import ubic.gemma.job.grid.util.JMSBroker;

import javax.jms.*;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * Client gets instance of this class in case when task is going to be run on remote worker host. The implementation
 * relies on 4 jms queues to send/receive data from remote worker. This proxy is synced with remote state only when used
 * by a client. The state is stored in messages on the jms queues.
 */
public class SubmittedTaskProxy<T extends TaskResult> implements SubmittedTask<T> {
    // Synced state fields
    // We have to make sure only one thread writes this field.
    private volatile Status status;
    private volatile Date submissionTime;
    private volatile Date startTime;
    private volatile Date finishTime;

    private java.util.Queue<String> syncedProgressUpdates = new ConcurrentLinkedQueue<String>();
    private TaskResult taskResult;

    private volatile String taskId;
    private volatile TaskCommand taskCommand;

    private boolean emailAlert = false;

    private final Object resultLock = new Object();
    private final Object lifeCycleStateLock = new Object();

    // ActiveMq queues to get the remote state.
    private Queue resultQueue;
    private Queue progressUpdatesQueue;
    private Queue lifeCycleQueue;
    // This is used to send 'cancel' request.
    private Queue controlQueue;

    @Autowired(required = false)
    @Qualifier("amqJmsTemplate")
    private JmsTemplate amqJmsTemplate;

    @Autowired
    private JMSBroker jmsBroker;

    // TODO: two separate locks: result and updates shouldn't block each other off.
    // Use case where one thread is waiting on the result.

    public SubmittedTaskProxy( TaskCommand taskCommand, JmsTemplate jmsTemplate ) {
        this.taskId = taskCommand.getTaskId();
        this.taskCommand = taskCommand;
        this.emailAlert = taskCommand.isEmailAlert();

        this.amqJmsTemplate = jmsTemplate;

        lifeCycleQueue = new ActiveMQQueue( "task.lifeCycle." + taskId );
        resultQueue = new ActiveMQQueue( "task.result." + taskId );
        progressUpdatesQueue = new ActiveMQQueue( "task.progress." + taskId );
        controlQueue = new ActiveMQQueue( "tasks.control" );

        this.status = Status.QUEUED;
        this.submissionTime = new Date();
    }

    @Override
    public java.util.Queue<String> getProgressUpdates() {
        syncProgressUpdates();
        return syncedProgressUpdates;
    }

    private void syncProgressUpdates() {
        synchronized ( lifeCycleStateLock ) {
            amqJmsTemplate.execute( new SessionCallback<Object>() {
                @Override
                public Object doInJms( Session session ) throws JMSException {
                    MessageConsumer consumer = session.createConsumer( progressUpdatesQueue );
                    Message progressMessage;
                    while ( ( progressMessage = consumer.receiveNoWait() ) != null ) {
                        ObjectMessage objectMessage = ( ObjectMessage ) progressMessage;
                        String message = ( String ) objectMessage.getObject();
                        syncedProgressUpdates.add( message );
                    }
                    return null;
                }
            } );
        }
    }

    @Override
    public boolean isDone() {
        syncLifeCycle();
        return ( this.status.equals( Status.DONE ) || this.status.equals( Status.FAILED ) );
    }

    private synchronized void syncLifeCycle() {
        synchronized ( lifeCycleStateLock ) {
            amqJmsTemplate.execute( new SessionCallback<Object>() {
                @Override
                public Object doInJms( Session session ) throws JMSException {
                    MessageConsumer consumer = session.createConsumer( lifeCycleQueue );
                    Message lifeCycleMessage;
                    while ( ( lifeCycleMessage = consumer.receiveNoWait() ) != null ) {
                        ObjectMessage objectMessage = ( ObjectMessage ) lifeCycleMessage;
                        TaskStatusUpdate statusUpdate = ( TaskStatusUpdate ) objectMessage.getObject();

                        applyStatusUpdate( statusUpdate );
                    }
                    return null;
                }
            }, true );
        }
    }

    private void applyStatusUpdate( TaskStatusUpdate statusUpdate ) {
        status = statusUpdate.getStatus();
        switch ( statusUpdate.getStatus() ) {
            case RUNNING:
                startTime = statusUpdate.getStatusChangeTime();
                break;
            case FAILED: case DONE:
            case CANCELLED:
                finishTime = statusUpdate.getStatusChangeTime();
                break;
        }
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    @Override
    public Date getStartTime() {
        syncLifeCycle();
        return this.startTime;
    }

    @Override
    public Date getSubmissionTime() {
        syncLifeCycle();
        return this.submissionTime;
    }

    @Override
    public Date getFinishTime() {
        syncLifeCycle();
        return this.finishTime;
    }

    @Override
    public T getResult() throws ExecutionException, InterruptedException {
        syncResult(); // blocks until result is available.
        return ( T ) taskResult;
    }

    // This blocks until result is received.
    private void syncResult() {
        synchronized ( resultLock ) {
            if ( taskResult != null ) return; // There could only be one result. Nothing to wait for.

            amqJmsTemplate.execute( new SessionCallback<Object>() {
                @Override
                public Object doInJms( Session session ) throws JMSException {
                    MessageConsumer consumer = session.createConsumer( resultQueue );
                    Message resultMessage = consumer.receive();
                    ObjectMessage objectMessage = ( ObjectMessage ) resultMessage;
                    taskResult = ( TaskResult ) objectMessage.getObject();
                    return null;
                }
            }, true );
        }
    }

    @Override
    public void cancel() {
        sendMessage( controlQueue, new TaskControl( taskId, TaskControl.Request.CANCEL ) );
    }

    @Override
    public Status getStatus() {
        syncLifeCycle();

        return this.status;
    }

    @Override
    public TaskCommand getCommand() {
        return this.taskCommand;
    }

    /**
     * Since this a proxy it is always a remote task.
     * 
     * @return true
     */
    @Override
    public boolean isRunningRemotely() {
        return true;
    }

    @Override
    public boolean isEmailAlert() {
        return emailAlert;
    }

    @Override
    public void addEmailAlert() {
        if ( emailAlert == true ) return; // Trying to prevent multiple email notifications being added.
        emailAlert = true;
        sendMessage( controlQueue, new TaskControl( taskId, TaskControl.Request.ADD_EMAIL_NOTIFICATION ) );
    }

    // TODO: this code is shared in a few places, extract it.
    private void sendMessage( Destination destination, final Serializable object ) {
        amqJmsTemplate.send( destination, new MessageCreator() {
            @Override
            public Message createMessage( Session session ) throws JMSException {
                ObjectMessage message = session.createObjectMessage( object );
                return message;
            }
        } );
    }
}