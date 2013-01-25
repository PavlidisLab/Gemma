package ubic.gemma.job.grid.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.RemoteTaskRunningService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskControl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 03/01/13
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
@Component("taskControlListener")
public class TaskControlListener implements MessageListener {

    private static final Log log = LogFactory.getLog( TaskControlListener.class );

    @Autowired RemoteTaskRunningService remoteTaskRunningService;

    @Override
    public void onMessage( Message message) {
        log.info( "Received task control message!");

        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            TaskControl taskControl = (TaskControl) objectMessage.getObject();
            switch (taskControl.getRequest()) {
                case CANCEL:
                    Future future = remoteTaskRunningService.getRunningTaskFuture( taskControl.getTaskId() );
                    if (future != null) {
                        future.cancel( true );
                    }
                    break;
                case ADD_EMAIL_NOTIFICATION:
                    // remoteTaskRunningService
                    //TODO: Implement me!
                    break;
            }

        } catch (JMSException e) {
            e.printStackTrace();  // FIXME?
        }
    }
}
