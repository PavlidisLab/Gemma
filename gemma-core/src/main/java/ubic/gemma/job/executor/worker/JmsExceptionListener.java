package ubic.gemma.job.executor.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * TODO: implement me! do something reasonable.
 * This is a way for listeners(TaskControlListener, TaskSubmissionListener) to be notified of connection problems.
 *
 */
@Component("jmsExceptionListener")
public class JmsExceptionListener implements ExceptionListener {

    private static final Log log = LogFactory.getLog( JmsExceptionListener.class );

    @Override
    public void onException( JMSException e ) {
        log.error( "JMS Exception: ", e );
    }
}
