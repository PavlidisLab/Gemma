package ubic.gemma.job.executor.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.TaskCommand;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

@Component("taskSubmissionListener")
public class TaskSubmissionListener implements MessageListener {
    private static final Log log = LogFactory.getLog( TaskSubmissionListener.class );

    @Autowired RemoteTaskRunningService remoteTaskRunningService;

    @Override
    public void onMessage( Message message) {
        log.info( "Received new remote task command!");

        ObjectMessage objectMessage = (ObjectMessage) message;
        TaskCommand taskCommand = null;
        try {
            taskCommand = (TaskCommand) objectMessage.getObject();
        } catch (JMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        log.info( "Submitting task command for execution.");

        remoteTaskRunningService.submit( taskCommand );
    }
}
