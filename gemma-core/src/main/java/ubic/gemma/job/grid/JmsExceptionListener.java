package ubic.gemma.job.grid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 05/02/13
 * Time: 9:34 AM
 * To change this template use File | Settings | File Templates.
 */
@Component("jmsExceptionListener")
public class JmsExceptionListener implements ExceptionListener {

    private static final Log log = LogFactory.getLog( JmsExceptionListener.class );

    @Override
    public void onException( JMSException e ) {
        log.error( "JMS Exception: ", e );
    }
}
